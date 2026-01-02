package com.am.common.investment.model.board;

/**
 * Enum representing different types of directors on a company board
 */
public enum DirectorType {
    EXECUTIVE("Executive"),
    NON_EXECUTIVE("Non-Executive"),
    INDEPENDENT("Independent"),
    CHAIRMAN("Chairman"),
    MANAGING_DIRECTOR("Managing Director"),
    COMPANY_SECRETARY("Company Secretary");
    
    private final String displayName;
    
    DirectorType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Maps a designation string to the appropriate DirectorType
     */
    public static DirectorType fromDesignation(String designation) {
        if (designation == null) {
            return null;
        }
        
        String lowerDesignation = designation.toLowerCase();
        
        if (lowerDesignation.contains("chairman") && lowerDesignation.contains("managing director")) {
            return CHAIRMAN; // Could also return a combination type if needed
        } else if (lowerDesignation.contains("chairman")) {
            return CHAIRMAN;
        } else if (lowerDesignation.contains("managing director")) {
            return MANAGING_DIRECTOR;
        } else if (lowerDesignation.contains("executive director")) {
            return EXECUTIVE;
        } else if (lowerDesignation.contains("independent")) {
            return INDEPENDENT;
        } else if (lowerDesignation.contains("non-exec") || lowerDesignation.contains("non exec")) {
            return NON_EXECUTIVE;
        } else if (lowerDesignation.contains("company sec")) {
            return COMPANY_SECRETARY;
        } else {
            // Default case
            return NON_EXECUTIVE;
        }
    }
}
