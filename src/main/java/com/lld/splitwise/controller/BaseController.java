package com.lld.splitwise.controller;

import com.lld.splitwise.exception.ConflictException;
import com.lld.splitwise.exception.ForbiddenException;
import com.lld.splitwise.exception.NotFoundException;
import com.lld.splitwise.exception.ValidationException;

import java.util.function.Supplier;

/**
 * Maps domain exceptions to HTTP status codes in ONE place
 * (like Spring's @ControllerAdvice), so services never know about HTTP.
 */
abstract class BaseController {

    protected <T> ApiResponse<T> handle(Supplier<T> call) {
        try {
            return ApiResponse.ok(call.get());
        } catch (ValidationException e) {
            return ApiResponse.error(400, e.getMessage());
        } catch (ForbiddenException e) {
            return ApiResponse.error(403, e.getMessage());
        } catch (NotFoundException e) {
            return ApiResponse.error(404, e.getMessage());
        } catch (ConflictException e) {
            return ApiResponse.error(409, e.getMessage());
        } catch (RuntimeException e) {
            return ApiResponse.error(500, "Internal error");
        }
    }
}
