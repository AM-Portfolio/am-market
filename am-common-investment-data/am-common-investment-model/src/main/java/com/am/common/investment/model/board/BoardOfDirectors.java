package com.am.common.investment.model.board;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.am.common.investment.model.equity.financial.BaseModel;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardOfDirectors extends BaseModel{
    private List<Director> directors;
    
    /**
     * Get all executive directors
     */
    public List<Director> getExecutiveDirectors() {
        return directors.stream()
                .filter(Director::isExecutive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all independent directors
     */
    public List<Director> getIndependentDirectors() {
        return directors.stream()
                .filter(Director::isIndependent)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all non-executive directors
     */
    public List<Director> getNonExecutiveDirectors() {
        return directors.stream()
                .filter(Director::isNonExecutive)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the chairman of the board
     */
    public Director getChairman() {
        return directors.stream()
                .filter(Director::isChairman)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get the managing director
     */
    public Director getManagingDirector() {
        return directors.stream()
                .filter(Director::isManagingDirector)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Group directors by their type
     */
    public Map<DirectorType, List<Director>> getDirectorsByType() {
        return directors.stream()
                .collect(Collectors.groupingBy(Director::getDirectorType));
    }
}
