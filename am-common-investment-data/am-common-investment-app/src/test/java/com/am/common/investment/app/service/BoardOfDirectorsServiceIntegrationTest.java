// package com.am.common.investment.app.service;

// import static org.assertj.core.api.Assertions.assertThat;

// import java.io.IOException;
// import java.time.LocalDateTime;
// import java.util.Optional;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;

// import com.am.common.investment.app.AmCommonInvestmentApplication;
// import com.am.common.investment.app.config.MongoTestConfig;
// import com.am.common.investment.app.config.TestContainersConfig;
// import com.am.common.investment.app.util.TestDataUtil;
// import com.am.common.investment.model.board.BoardOfDirectors;
// import com.am.common.investment.model.board.Director;
// import com.am.common.investment.persistence.repository.companyprofile.BoardOfDirectorsRepository;
// import com.am.common.investment.service.StockFinancialPerformanceService;

// /**
//  * Integration test for BoardOfDirectors functionality using TestContainers
//  */
// @SpringBootTest(classes = {
//     AmCommonInvestmentApplication.class,
//     MongoTestConfig.class,
//     TestContainersConfig.class
// })
// @ActiveProfiles("test")
// public class BoardOfDirectorsServiceIntegrationTest {

//     @Autowired
//     private StockFinancialPerformanceService stockFinancialPerformanceService;
    
//     @Autowired
//     private BoardOfDirectorsRepository boardOfDirectorsRepository;

//     private BoardOfDirectors boardOfDirectors;

//     @BeforeEach
//     void cleanup() {
//         // Clean up any existing documents
//         boardOfDirectorsRepository.deleteAll();
//     }

//     @BeforeEach
//     void setup() throws IOException {
//         boardOfDirectors = TestDataUtil.readBoardOfDirectorsFromResource("boardofdirectors.json");
//     }

//     @Test
//     void shouldSaveAndRetrieveBoardOfDirectors() {
//         // Given
//         String symbol = boardOfDirectors.getSymbol();
//         boardOfDirectors.setAudit(TestDataUtil.createAudit(LocalDateTime.now(), "test"));
//         BoardOfDirectors savedBoardOfDirectors_1 = stockFinancialPerformanceService.saveBoardOfDirectors(boardOfDirectors);


//         BoardOfDirectors boardOfDirectors_2 = boardOfDirectors;
//         boardOfDirectors_2.setAudit(TestDataUtil.createAudit(LocalDateTime.now(), "Munish"));
//         BoardOfDirectors savedBoardOfDirectors_2 = stockFinancialPerformanceService.saveBoardOfDirectors(boardOfDirectors_2);


//         Optional<BoardOfDirectors> retrievedBoardOfDirectors = stockFinancialPerformanceService.getBoardOfDirectors(symbol);
        
//         // Then
//         assertThat(retrievedBoardOfDirectors).isPresent();
//         assertThat(retrievedBoardOfDirectors.get().getSymbol()).isEqualTo(symbol);
//         assertThat(retrievedBoardOfDirectors.get().getDirectors().size()).isEqualTo(boardOfDirectors.getDirectors().size());
        
//         // Verify the first director's details
//         Director originalDirector = boardOfDirectors.getDirectors().get(0);
//         Director retrievedDirector = retrievedBoardOfDirectors.get().getDirectors().get(0);
        
//         assertThat(retrievedDirector.getDirName()).isEqualTo(originalDirector.getDirName());
//         assertThat(retrievedDirector.getReportedDsg()).isEqualTo(originalDirector.getReportedDsg());
//         assertThat(retrievedDirector.getDirectorType()).isEqualTo(originalDirector.getDirectorType());
//         assertThat(retrievedDirector.getCompanyId()).isEqualTo(originalDirector.getCompanyId());
//         assertThat(retrievedDirector.getAppointmentDate()).isEqualTo(originalDirector.getAppointmentDate());
//         assertThat(retrievedDirector.getLastReelectionDate()).isEqualTo(originalDirector.getLastReelectionDate());
        
//         // Verify versioning
//         assertThat(savedBoardOfDirectors_1.getVersion()).isEqualTo(2);
//         assertThat(savedBoardOfDirectors_2.getVersion()).isEqualTo(2);
//         assertThat(retrievedBoardOfDirectors.get().getVersion()).isEqualTo(2);

//         Optional<BoardOfDirectors> boardOfDirectors = stockFinancialPerformanceService.getBoardOfDirectors("AMZ");
        
//         // Then
//         assertThat(boardOfDirectors).isNotPresent();

//     }
// }
