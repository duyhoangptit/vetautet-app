
package com.vetautet.app.application.notification.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.vetautet.app.domain.notification.model.NotificationChannelCode;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SendNotificationCommand {

    /** Business entity this notification relates to (e.g. booking_order_id, user_id). */
    private UUID referenceId;

    /** Type of the business entity (e.g. "BOOKING_ORDER", "USER"). */
    private String referenceType;

    private NotificationChannelCode channelCode;

    /**
     * Template code to look up (e.g. "OTP_VERIFICATION").
     * Null when the caller builds the content manually via {@code subject} and {@code content}.
     */
    private String templateCode;

    /** Locale for template lookup. Defaults to "vi" when null. */
    @Builder.Default
    private String locale = "vi";

    /**
     * Primary recipient: email / phone / device token / chat_id.
     * Required for non-EMAIL channels.
     */
    private String recipient;

    /**
     * Additional TO recipients from the caller.
     * Merged with template.toList (union, no duplicates) at send time.
     * Applies to EMAIL channel only.
     */
    private List<String> toList;

    /** Additional CC recipients. Merged with template.ccList. */
    private List<String> ccList;

    /** Additional BCC recipients. Merged with template.bccList. */
    private List<String> bccList;

    /**
     * Variables for template rendering ({{variable_name}} substitution).
     * Also used as content snapshot when no template is used.
     */
    private Map<String, String> variables;

    /**
     * Pre-built subject. Used when templateCode is null.
     * When templateCode is set, this field is ignored (subject is rendered from template).
     */
    private String subject;

    /**
     * Pre-built body content. Required when templateCode is null.
     * When templateCode is set, this field is ignored (body is rendered from template).
     */
    private String content;

    /**
     * Correlation ID from the triggering outbox event (for dedup on re-delivery).
     */
    private String eventId;

    /** Max number of retry attempts. Defaults to 3. */
    @Builder.Default
    private short maxRetries = 3;
}