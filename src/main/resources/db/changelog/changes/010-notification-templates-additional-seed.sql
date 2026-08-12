-- Liquibase formatted SQL

-- ============================================================
-- changeset vetautet:010-notification-templates-register-booking
-- comment: Add REGISTER_ACTIVATION and BOOKING_HOLD templates
-- ============================================================
INSERT INTO notification_templates (
    template_code, channel_code, locale, template_name,
    subject_template, body_template, variable_keys, description
) VALUES
      (
          'REGISTER_ACTIVATION', 'EMAIL', 'vi',
          'Kích hoạt tài khoản',
          '[VeTauTet] Kích hoạt tài khoản của bạn',
          E'Xin chào {{full_name}},\n\nCảm ơn bạn đã đăng ký tài khoản VeTauTet!\n\nVui lòng nhấn vào đường link sau để kích hoạt tài khoản:\n\n{{activation_link}}\n\nĐường link có hiệu lực đến: {{expires_at}}\n\nNếu bạn không thực hiện đăng ký này, vui lòng bỏ qua email.\n\nTrân trọng,\nVeTauTet',
          '{full_name,activation_link,expires_at}',
          'Email gửi link kích hoạt tài khoản sau khi đăng ký'
      ),
      (
          'BOOKING_HOLD', 'EMAIL', 'vi',
          'Xác nhận giữ chỗ',
          '[VeTauTet] Giữ chỗ thành công - {{order_code}}',
          E'Xin chào {{full_name}},\n\nYêu cầu giữ chỗ của bạn đã được ghi nhận thành công.\n\nMã đặt chỗ: {{order_code}}\nChuyến: {{departure_code}} — Ngày {{departure_date}}\nKhởi hành lúc: {{planned_departure_at}}\nSố vé: {{item_count}}\nTổng tiền: {{total_amount}} {{currency_code}}\n\nChỗ được giữ đến: {{hold_expires_at}}\nPhương thức thanh toán: {{payment_method}}\n\nVui lòng hoàn tất thanh toán trước thời gian hết hạn giữ chỗ để xác nhận đặt vé.\n\nTrân trọng,\nVeTauTet',
          '{full_name,order_code,departure_code,departure_date,planned_departure_at,item_count,total_amount,currency_code,hold_expires_at,payment_method}',
          'Email xác nhận giữ chỗ thành công, nhắc user thanh toán trước khi hết hạn'
      ),
      (
          'BOOKING_HOLD', 'SMS', 'vi',
          'Xác nhận giữ chỗ qua SMS',
          NULL,
          '[VeTauTet] Giu cho {{order_code}} thanh cong. {{total_amount}} {{currency_code}}. Het han luc {{hold_expires_at}}. Vui long thanh toan dung han.',
          '{order_code,total_amount,currency_code,hold_expires_at}',
          'SMS xác nhận giữ chỗ (không dấu)'
      );

-- rollback DELETE FROM notification_templates WHERE template_code IN ('REGISTER_ACTIVATION','BOOKING_HOLD');
