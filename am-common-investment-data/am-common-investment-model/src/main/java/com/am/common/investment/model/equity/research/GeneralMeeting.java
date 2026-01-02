package com.am.common.investment.model.equity.research;

import java.time.LocalDate;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralMeeting {
    /** Date when the meeting was announced */
    private LocalDate announcementDate;
    
    /** Date of the general meeting */
    private LocalDate meetingDate;
    
    /** Description of the meeting */
    private String description;
    
    /** Type of meeting (e.g., "Annual General Meeting", "Extraordinary General Meeting") */
    private String type;
}
