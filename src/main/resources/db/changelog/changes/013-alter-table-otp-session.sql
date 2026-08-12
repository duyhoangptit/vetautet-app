--liquibase formatted sql

--changeset vetautet:013-alter-table-otp-session
--comment: Update schema for OTP session management

-- 1. Thêm cột data length = 1000, type text vào bảng otp_sessions
ALTER TABLE otp_sessions
ADD COLUMN data TEXT;

-- 2. Remove cột pending_password_hash vì không còn cần thiết
ALTER TABLE otp_sessions
DROP COLUMN pending_password_hash;
