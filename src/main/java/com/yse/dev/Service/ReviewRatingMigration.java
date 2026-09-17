package com.yse.dev.Service;
import com.yse.dev.Repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/** ddl-auto=update adds a nullable column; existing rows remain NULL until converted. */
@Component @RequiredArgsConstructor
public class ReviewRatingMigration implements ApplicationRunner {
    private final ReviewRepository reviews;
    @Override @Transactional
    public void run(ApplicationArguments args) {
        reviews.convertLegacyRatingsToTen();
        if(reviews.countInvalidRatingScale()>0) throw new IllegalStateException("환산할 수 없는 리뷰 별점이 있습니다. DB의 review.rating과 rating_scale을 확인해 주세요. 자동 환산은 원래 1~5점인 행만 처리합니다.");
    }
}
