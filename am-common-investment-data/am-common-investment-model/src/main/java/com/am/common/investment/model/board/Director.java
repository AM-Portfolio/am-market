package com.am.common.investment.model.board;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Director {
    private String dirName;
    private String reportedDsg;
    private DirectorType directorType;
    private String companyId;
    private LocalDate appointmentDate;
    private LocalDate lastReelectionDate;
    
    
    public boolean isExecutive() {
        return DirectorType.EXECUTIVE.equals(this.directorType);
    }
    
    public boolean isIndependent() {
        return DirectorType.INDEPENDENT.equals(this.directorType);
    }
    
    public boolean isChairman() {
        return DirectorType.CHAIRMAN.equals(this.directorType);
    }
    
    public boolean isManagingDirector() {
        return DirectorType.MANAGING_DIRECTOR.equals(this.directorType);
    }
    
    public boolean isNonExecutive() {
        return DirectorType.NON_EXECUTIVE.equals(this.directorType);
    }
}
