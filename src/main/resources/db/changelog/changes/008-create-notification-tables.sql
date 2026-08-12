-- Liquibase formatted SQL

-- ============================================================
-- changeset vetautet:008-notification-channels
-- comment: Master data table for supported notification channels
-- ============================================================
CREATE TABLE notification_channels (
                                       channel_code    VARCHAR(50)     NOT NULL,
                                       channel_name    VARCHAR(100)    NOT NULL,
                                       description     TEXT,
                                       is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
                                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                       CONSTRAINT pk_notification_channels PRIMARY KEY (channel_code)
);

INSERT INTO notification_channels (channel_code, channel_name, description) VALUES
                                                                                ('EMAIL',         'Email',                    'Send notification via email'),
                                                                                ('SMS',           'SMS',                      'Send notification via SMS'),
                                                                                ('TELEGRAM',      'Telegram Bot',             'Send notification via Telegram bot'),
                                                                                ('FIREBASE_PUSH', 'Firebase Push',            'Send push notification via Firebase Cloud Messaging'),
                                                                                ('ZALO_OA',       'Zalo Official Account',    'Send notification via Zalo OA message');

-- rollback DROP TABLE notification_channels;

-- ============================================================
-- changeset vetautet:008-notification-templates
-- comment: Notification content templates with variable substitution support
-- ============================================================
CREATE TABLE notification_templates (
                                        notification_template_id    UUID            NOT NULL DEFAULT gen_random_uuid(),
                                        template_code               VARCHAR(100)    NOT NULL,
                                        channel_code                VARCHAR(50)     NOT NULL,
                                        locale                      VARCHAR(10)     NOT NULL DEFAULT 'vi',
                                        template_name               VARCHAR(255)    NOT NULL,
    -- subject_template: nullable, used for channels that support subject (e.g. EMAIL)
    -- supports {{variable_name}} placeholder, e.g. [VeTauTet] Xác nhận OTP - {{booking_code}}
                                        subject_template            TEXT,
    -- body_template: supports {{variable_name}} placeholder
    -- e.g. Xin chào {{full_name}}, mã OTP của bạn là {{otp_code}}, hết hạn sau {{expires_minutes}} phút.
                                        body_template               TEXT            NOT NULL,
    -- variable_keys: declared list of expected variable names, e.g. '{full_name,otp_code,expires_minutes}'
    -- used for validation before rendering; application must supply all declared keys
                                        variable_keys               TEXT[]          NOT NULL DEFAULT '{}',
                                        description                 TEXT,
                                        is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,
                                        created_date                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                        last_modified_date          TIMESTAMPTZ,
                                        created_by                  VARCHAR(50),
                                        last_modified_by            VARCHAR(50),
                                        version                     BIGINT,
                                        CONSTRAINT pk_notification_templates
                                            PRIMARY KEY (notification_template_id),
                                        CONSTRAINT uq_notification_templates_code_channel_locale
                                            UNIQUE (template_code, channel_code, locale),
                                        CONSTRAINT fk_notification_templates_channel
                                            FOREIGN KEY (channel_code) REFERENCES notification_channels (channel_code)
);

CREATE INDEX idx_notification_templates_lookup
    ON notification_templates (template_code, channel_code, locale)
    WHERE is_active = TRUE;

-- rollback DROP INDEX idx_notification_templates_lookup;
-- rollback DROP TABLE notification_templates;

-- ============================================================
-- changeset vetautet:008-notification-templates-seed
-- comment: Seed sample templates for OTP and booking flows
-- ============================================================
INSERT INTO notification_templates (
    template_code, channel_code, locale, template_name,
    subject_template, body_template, variable_keys, description
) VALUES
      (
          'OTP_VERIFICATION', 'EMAIL', 'vi',
          'Xác thực OTP qua Email',
          '[VeTauTet] Mã xác thực của bạn',
          E'Xin chào {{full_name}},\n\nMã OTP của bạn là: {{otp_code}}\nMã có hiệu lực trong {{expires_minutes}} phút.\n\nVui lòng không chia sẻ mã này với bất kỳ ai.\n\nTrân trọng,\nVeTauTet',
          '{full_name,otp_code,expires_minutes}',
          'OTP gửi khi đăng nhập hoặc xác thực tài khoản qua email'
      ),
      (
          'OTP_VERIFICATION', 'SMS', 'vi',
          'Xác thực OTP qua SMS',
          NULL,
          '[VeTauTet] Ma OTP: {{otp_code}} (het han sau {{expires_minutes}} phut). Khong chia se ma nay.',
          '{otp_code,expires_minutes}',
          'OTP gửi khi đăng nhập qua SMS (không dấu để tương thích SMS gateway)'
      ),
      (
          'BOOKING_CONFIRMED', 'EMAIL', 'vi',
          'Xác nhận đặt vé thành công',
          '[VeTauTet] Xác nhận đặt vé - {{booking_code}}',
          E'Xin chào {{full_name}},\n\nĐơn đặt vé của bạn đã được xác nhận.\n\nMã đặt chỗ: {{booking_code}}\nChuyến tàu: {{departure_name}}\nNgày khởi hành: {{departure_date}}\nGhế: {{seat_info}}\nTổng tiền: {{total_amount}} VNĐ\n\nVui lòng xuất trình mã đặt chỗ tại quầy hoặc cổng soát vé.\n\nChúc bạn có chuyến đi vui vẻ!\nVeTauTet',
          '{full_name,booking_code,departure_name,departure_date,seat_info,total_amount}',
          'Email xác nhận sau khi thanh toán thành công'
      ),
      (
          'BOOKING_CONFIRMED', 'SMS', 'vi',
          'Xác nhận đặt vé qua SMS',
          NULL,
          '[VeTauTet] Ve {{booking_code}} da xac nhan. Tau {{departure_name}} ngay {{departure_date}}, ghe {{seat_info}}.',
          '{booking_code,departure_name,departure_date,seat_info}',
          'SMS xác nhận đặt vé (không dấu)'
      ),
      (
          'PAYMENT_SUCCESS', 'EMAIL', 'vi',
          'Thanh toán thành công',
          '[VeTauTet] Thanh toán thành công - {{booking_code}}',
          E'Xin chào {{full_name}},\n\nGiao dịch thanh toán của bạn đã thành công.\n\nMã đặt chỗ: {{booking_code}}\nSố tiền: {{amount}} VNĐ\nPhương thức: {{payment_method}}\nMã giao dịch: {{transaction_id}}\nThời gian: {{paid_at}}\n\nTrân trọng,\nVeTauTet',
          '{full_name,booking_code,amount,payment_method,transaction_id,paid_at}',
          'Email xác nhận thanh toán'
      ),
      (
          'BOOKING_CANCELLED', 'EMAIL', 'vi',
          'Thông báo hủy vé',
          '[VeTauTet] Vé {{booking_code}} đã được hủy',
          E'Xin chào {{full_name}},\n\nVé {{booking_code}} của bạn đã được hủy thành công.\n\nLý do: {{cancel_reason}}\nSố tiền hoàn: {{refund_amount}} VNĐ\nThời gian hoàn dự kiến: {{refund_eta}}\n\nTrân trọng,\nVeTauTet',
          '{full_name,booking_code,cancel_reason,refund_amount,refund_eta}',
          'Email thông báo hủy vé và hoàn tiền'
      ),
      (
          'FORGOT_PASSWORD', 'EMAIL', 'vi',
          'Yêu cầu đặt lại mật khẩu',
          '[VeTauTet] Đặt lại mật khẩu của bạn',
          E'Xin chào {{full_name}},\n\nChúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản {{email}}.\n\nMã xác nhận: {{reset_code}}\nHiệu lực: {{expires_minutes}} phút.\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.\n\nTrân trọng,\nVeTauTet',
          '{full_name,email,reset_code,expires_minutes}',
          'Email gửi mã reset mật khẩu'
      );

-- rollback DELETE FROM notification_templates WHERE template_code IN ('OTP_VERIFICATION','BOOKING_CONFIRMED','PAYMENT_SUCCESS','BOOKING_CANCELLED','FORGOT_PASSWORD');

-- ============================================================
-- changeset vetautet:008-notification-logs
-- comment: Log of every notification dispatch attempt with status tracking and retry support
-- ============================================================
CREATE TABLE notification_logs (
                                   notification_log_id     UUID            NOT NULL DEFAULT gen_random_uuid(),
    -- event_id: correlation to outbox_events.event_id for dedup and tracing
                                   event_id                VARCHAR(100),
    -- reference_id + reference_type: business entity this notification relates to
                                   reference_id            UUID,
                                   reference_type          VARCHAR(100),
                                   channel_code            VARCHAR(50)     NOT NULL,
    -- template_code: NULL when content was built by the application without a template
                                   template_code           VARCHAR(100),
    -- recipient: email address / phone number / device token / telegram chat_id
                                   recipient               VARCHAR(500)    NOT NULL,
                                   subject                 TEXT,
    -- content: final rendered body after variable substitution
                                   content                 TEXT            NOT NULL,
    -- variables: JSON snapshot of variable map used at render time, for audit/debug
                                   variables               JSONB,
                                   status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    -- provider_code: actual provider used, e.g. SENDGRID, TWILIO, FIREBASE, BOT_TELEGRAM
                                   provider_code           VARCHAR(50),
                                   provider_message_id     VARCHAR(255),
                                   sent_at                 TIMESTAMPTZ,
                                   delivered_at            TIMESTAMPTZ,
                                   failed_at               TIMESTAMPTZ,
                                   error_code              VARCHAR(100),
                                   error_message           TEXT,
                                   retry_count             SMALLINT        NOT NULL DEFAULT 0,
                                   max_retries             SMALLINT        NOT NULL DEFAULT 3,
    -- next_retry_at: NULL means no retry scheduled (success, skipped, or max retries reached)
                                   next_retry_at           TIMESTAMPTZ,
                                   created_date            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                   last_modified_date      TIMESTAMPTZ,
                                   created_by              VARCHAR(50),
                                   last_modified_by        VARCHAR(50),
                                   version                 BIGINT,
                                   CONSTRAINT pk_notification_logs
                                       PRIMARY KEY (notification_log_id),
                                   CONSTRAINT chk_notification_logs_status
                                       CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'SKIPPED')),
                                   CONSTRAINT fk_notification_logs_channel
                                       FOREIGN KEY (channel_code) REFERENCES notification_channels (channel_code)
);

-- Retry worker: poll PENDING/FAILED logs that are due for retry
CREATE INDEX idx_notification_logs_retry
    ON notification_logs (status, next_retry_at)
    WHERE status IN ('PENDING', 'FAILED') AND next_retry_at IS NOT NULL;

-- Lookup notifications by business entity
CREATE INDEX idx_notification_logs_reference
    ON notification_logs (reference_id, reference_type)
    WHERE reference_id IS NOT NULL;

-- Dedup check from outbox relay
CREATE INDEX idx_notification_logs_event_id
    ON notification_logs (event_id)
    WHERE event_id IS NOT NULL;

-- Time-range queries and purge jobs
CREATE INDEX idx_notification_logs_created_at
    ON notification_logs (created_date);

-- rollback DROP INDEX idx_notification_logs_created_at;
-- rollback DROP INDEX idx_notification_logs_event_id;
-- rollback DROP INDEX idx_notification_logs_reference;
-- rollback DROP INDEX idx_notification_logs_retry;
-- rollback DROP TABLE notification_logs;
