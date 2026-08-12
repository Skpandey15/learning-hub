package com.learninghub.security;

public enum LearningRole {
    CANDIDATE,
    INTERVIEWER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
