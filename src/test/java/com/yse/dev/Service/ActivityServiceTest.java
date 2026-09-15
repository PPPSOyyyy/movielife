package com.yse.dev.Service;

import com.yse.dev.DTO.LoginDto;
import com.yse.dev.Entity.Favorite;
import com.yse.dev.Entity.Member;
import com.yse.dev.Entity.Review;
import com.yse.dev.Repository.FavoriteRepository;
import com.yse.dev.Repository.MemberRepository;
import com.yse.dev.Repository.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityServiceTest {
    private MemberRepository members;
    private FavoriteRepository favorites;
    private ReviewRepository reviews;
    private Member member;

    @BeforeEach
    void setup() {
        members = mock(MemberRepository.class);
        favorites = mock(FavoriteRepository.class);
        reviews = mock(ReviewRepository.class);
        member = new Member();
        member.setUserId("movie_user");
        member.setNickname("영화산책");
        member.setPassword("Legacy1!");
        when(members.findByUserIdForUpdate("movie_user")).thenReturn(Optional.of(member));
    }

    @Test
    void withdrawDeletesActivityBeforeAccount() {
        new MemberService(members, favorites, reviews).withdraw("movie_user", "Legacy1!");
        var order = inOrder(favorites, reviews, members);
        order.verify(favorites).deleteByUserId("movie_user");
        order.verify(reviews).deleteByUserId("movie_user");
        order.verify(members).delete(member);
    }

    @Test
    void wrongPasswordDoesNotDeleteAnything() {
        assertThrows(IllegalArgumentException.class,
                () -> new MemberService(members, favorites, reviews).withdraw("movie_user", "wrong"));
        verifyNoInteractions(favorites, reviews);
        verify(members, never()).delete(any());
    }

    @Test
    void legacyPasswordMigratesOnlyAfterSuccessfulLogin() {
        when(members.findByUserId("movie_user")).thenReturn(Optional.of(member));
        LoginDto dto = new LoginDto();
        dto.setUserId("movie_user");
        dto.setPassword("wrong");
        MemberService service = new MemberService(members, favorites, reviews);
        assertThrows(IllegalArgumentException.class, () -> service.login(dto));
        assertEquals("Legacy1!", member.getPassword());
        dto.setPassword("Legacy1!");
        service.login(dto);
        assertTrue(new BCryptPasswordEncoder().matches("Legacy1!", member.getPassword()));
    }

    @Test
    void repeatedFavoriteReturnsExistingEntry() {
        Favorite existing = new Favorite();
        existing.setMovieId(42L);
        when(favorites.findByUserIdAndMovieId("movie_user", 42L)).thenReturn(Optional.of(existing));
        Favorite result = new FavoriteService(favorites, members).addFavorite("movie_user", 42L);
        assertSame(existing, result);
        verify(members).findByUserIdForUpdate("movie_user");
        verify(favorites, never()).save(any());
    }

    @Test
    void invalidMovieCannotBeFavorited() {
        assertThrows(IllegalArgumentException.class,
                () -> new FavoriteService(favorites, members).addFavorite("movie_user", -1L));
        verifyNoInteractions(favorites);
    }

    @Test
    void duplicateReviewCannotBeWritten() {
        when(reviews.countByUserIdAndMovieId("movie_user", 42L)).thenReturn(1L);
        assertThrows(IllegalArgumentException.class,
                () -> new ReviewService(reviews, members).createReview("movie_user", 42L, 5, "감상"));
        verify(reviews, never()).save(any());
    }

    @Test
    void anotherUsersReviewCannotBeEditedOrDeleted() {
        Review review = new Review();
        review.setUserId("someone_else");
        when(reviews.findById(7L)).thenReturn(Optional.of(review));
        ReviewService service = new ReviewService(reviews, members);
        assertThrows(IllegalArgumentException.class, () -> service.updateReview(7L, "movie_user", 5, "변경"));
        assertThrows(IllegalArgumentException.class, () -> service.deleteReview(7L, "movie_user"));
        verify(reviews, never()).save(any());
        verify(reviews, never()).delete(any());
    }
}
