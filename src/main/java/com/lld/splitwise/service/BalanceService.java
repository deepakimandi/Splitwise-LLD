package com.lld.splitwise.service;

import com.lld.splitwise.dto.UserBalanceSummary;
import com.lld.splitwise.model.Group;
import com.lld.splitwise.model.User;
import com.lld.splitwise.repository.GroupRepository;
import com.lld.splitwise.repository.UserRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read side. Balances are stored ONLY per group; the user's overall balance is computed
 * by summing their groups. This avoids keeping two ledgers in sync (global + group).
 * At scale, cache the summary and refresh it from events (a CQRS read model).
 */
public class BalanceService {
    private final GroupRepository groups;
    private final Lookup lookup;

    public BalanceService(GroupRepository groups, UserRepository users) {
        this.groups = groups;
        this.lookup = new Lookup(users, groups);
    }

    public Map<User, BigDecimal> groupBalances(String requesterId, String groupId) {
        Group group = lookup.group(groupId);
        lookup.requireMember(group, requesterId);
        return group.netBalances();
    }

    /** Positive => other owes userId in this group. */
    public BigDecimal balanceBetween(String groupId, String userId, String otherUserId) {
        return lookup.group(groupId).balanceBetween(userId, otherUserId);
    }

    public UserBalanceSummary userSummary(String userId) {
        lookup.user(userId);
        Map<String, BigDecimal> byGroup = new LinkedHashMap<>();
        BigDecimal overall = BigDecimal.ZERO;
        for (Group group : groups.findByMember(userId)) {
            BigDecimal net = group.netBalanceOf(userId);
            byGroup.put(group.getName(), net);
            overall = overall.add(net);
        }
        return new UserBalanceSummary(userId, overall, byGroup);
    }
}
