// package com.am.marketdata.tradebrain.mapper;

// import java.util.List;
// import java.util.stream.Collectors;

// import com.am.common.investment.model.board.BoardOfDirectors;
// import com.am.common.investment.model.board.Director;
// import com.am.common.investment.model.board.DirectorType;
// import com.am.marketdata.common.model.events.BoardOfDirector;
// import com.am.marketdata.common.model.events.BoardOfDirector.DesignationType;

// import org.springframework.stereotype.Component;

// import lombok.RequiredArgsConstructor;

// /**
//  * Mapper for converting between BoardOfDirectors and BoardOfDirector
//  */
// @Component
// @RequiredArgsConstructor
// public class MarketBoardOfDirectorsMapper {
    
//     /**
//      * Create BoardOfDirectors from symbol and list of directors
//      * 
//      * @param symbol Stock symbol
//      * @param directors List of directors
//      * @return BoardOfDirectors object
//      */
//     public BoardOfDirectors toBoardOfDirectors(String symbol, List<BoardOfDirector> directors) {
//         if (directors == null) {
//             return null;
//         }
//         return BoardOfDirectors.builder()
//             .companyId(symbol)
//             .directors(toDirectors(symbol, directors))
//             .build();
//     }

//     public List<Director> toDirectors(String symbol,List<BoardOfDirector> directors) {
//         if (directors == null) {
//             return null;
//         }
//         return directors.stream()
//         .map(director -> getDirector(symbol, director))
//         .collect(Collectors.toList());
//     }

//     private Director getDirector(String symbol,BoardOfDirector director) {
//         return Director.builder()
//             .dirName(director.getDirectorName())
//             .reportedDsg(director.getDesignation())
//             .companyId(symbol)
//             //.directorType(getDirectorType(director.getDesignationType()))
//             .build();
//     }

//     private DirectorType getDirectorType(DesignationType designationType) {
//         if (designationType == null) {
//             return null;
//         }
//         return DirectorType.fromDesignation(designationType.name());
//     }
// }
