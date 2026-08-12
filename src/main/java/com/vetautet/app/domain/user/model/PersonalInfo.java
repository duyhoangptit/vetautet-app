package com.vetautet.app.domain.user.model;

import java.util.Objects;

/**
 * Value Object representing personal information
 * Immutable and contains user's personal details
 */
public final class PersonalInfo {

    private final String firstName;
    private final String lastName;
    private final String mobileCountryCode;
    private final String mobileNumber;
    private final String avatarUrl;

    private PersonalInfo(Builder builder) {
        Objects.requireNonNull(builder.firstName, "First name cannot be null");
        Objects.requireNonNull(builder.lastName, "Last name cannot be null");

        if (builder.firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }

        if (builder.lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }

        this.firstName = builder.firstName.trim();
        this.lastName = builder.lastName.trim();
        this.mobileCountryCode = builder.mobileCountryCode;
        this.mobileNumber = builder.mobileNumber;
        this.avatarUrl = builder.avatarUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getMobileCountryCode() {
        return mobileCountryCode;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean hasMobileNumber() {
        return mobileNumber != null && !mobileNumber.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PersonalInfo that = (PersonalInfo) o;
        return Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(mobileCountryCode, that.mobileCountryCode) &&
                Objects.equals(mobileNumber, that.mobileNumber) &&
                Objects.equals(avatarUrl, that.avatarUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, mobileCountryCode, mobileNumber, avatarUrl);
    }

    @Override
    public String toString() {
        return "PersonalInfo{" +
                "fullName='" + getFullName() + '\'' +
                ", mobile='" + (hasMobileNumber() ? mobileCountryCode + mobileNumber : "N/A") + '\'' +
                '}';
    }

    public static class Builder {
        private String firstName;
        private String lastName;
        private String mobileCountryCode;
        private String mobileNumber;
        private String avatarUrl;

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder mobileCountryCode(String mobileCountryCode) {
            this.mobileCountryCode = mobileCountryCode;
            return this;
        }

        public Builder mobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
            return this;
        }

        public Builder avatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        public PersonalInfo build() {
            return new PersonalInfo(this);
        }
    }
}
