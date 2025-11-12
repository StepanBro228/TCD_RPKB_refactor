package com.step.tcd_rpkb.domain.model;

public class User {
    private final String fullName;
    private final String role;
    private final String userGuid;

    public User(String fullName, String role, String userGuid) {
        this.fullName = fullName;
        this.role = role;
        this.userGuid = userGuid;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getUserGuid() {
        return userGuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (fullName != null ? !fullName.equals(user.fullName) : user.fullName != null)
            return false;
        if (role != null ? !role.equals(user.role) : user.role != null) return false;
        return userGuid != null ? userGuid.equals(user.userGuid) : user.userGuid == null;
    }

    @Override
    public int hashCode() {
        int result = fullName != null ? fullName.hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (userGuid != null ? userGuid.hashCode() : 0);
        return result;
    }
} 