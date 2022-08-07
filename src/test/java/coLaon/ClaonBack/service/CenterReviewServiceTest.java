package coLaon.ClaonBack.service;

import coLaon.ClaonBack.center.domain.Center;
import coLaon.ClaonBack.center.domain.CenterImg;
import coLaon.ClaonBack.center.domain.CenterReview;
import coLaon.ClaonBack.center.domain.Charge;
import coLaon.ClaonBack.center.domain.OperatingTime;
import coLaon.ClaonBack.center.domain.SectorInfo;
import coLaon.ClaonBack.center.dto.ReviewCreateRequestDto;
import coLaon.ClaonBack.center.dto.ReviewFindResponseDto;
import coLaon.ClaonBack.center.dto.ReviewListFindResponseDto;
import coLaon.ClaonBack.center.dto.ReviewResponseDto;
import coLaon.ClaonBack.center.dto.ReviewUpdateRequestDto;
import coLaon.ClaonBack.center.repository.CenterRepository;
import coLaon.ClaonBack.center.repository.ReviewRepository;
import coLaon.ClaonBack.center.repository.ReviewRepositorySupport;
import coLaon.ClaonBack.center.service.CenterReviewService;
import coLaon.ClaonBack.common.domain.PaginationFactory;
import coLaon.ClaonBack.common.exception.BadRequestException;
import coLaon.ClaonBack.common.exception.ErrorCode;
import coLaon.ClaonBack.common.exception.UnauthorizedException;
import coLaon.ClaonBack.user.domain.User;
import coLaon.ClaonBack.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class CenterReviewServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    CenterRepository centerRepository;
    @Mock
    ReviewRepository reviewRepository;
    @Mock
    ReviewRepositorySupport reviewRepositorySupport;

    @Spy
    PaginationFactory paginationFactory = new PaginationFactory();

    @InjectMocks
    CenterReviewService centerReviewService;

    private User user, user2;
    private Center center;
    private CenterReview review1, review2;

    @BeforeEach
    void setUp() {
        this.user = User.of(
                "test@gmail.com",
                "1234567222",
                "userNickname2",
                "경기도",
                "성남시",
                "",
                "",
                "instagramId2"
        );
        ReflectionTestUtils.setField(this.user, "id", "userId");

        this.user2 = User.of(
                "test2@gmail.com",
                "1234567222",
                "userNickname3",
                "경기도",
                "성남시",
                "",
                "",
                "instagramId3"
        );
        ReflectionTestUtils.setField(this.user2, "id", "userId2");

        this.center = Center.of(
                "test",
                "test",
                "010-1234-1234",
                "https://test.com",
                "https://instagram.com/test",
                "https://youtube.com/channel/test",
                List.of(new CenterImg("img test")),
                List.of(new OperatingTime("매일", "10:00", "23:00")),
                "facilities test",
                List.of(new Charge("자유 패키지", "330,000")),
                "charge img test",
                "hold info img test",
                List.of(new SectorInfo("test sector", "1/1", "1/2"))
        );
        ReflectionTestUtils.setField(this.center, "id", "center id");

        this.review1 = CenterReview.of(5, "testContent1", this.user, this.center);
        ReflectionTestUtils.setField(this.review1, "id", "review1Id");
        ReflectionTestUtils.setField(this.review1, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(this.review1, "updatedAt", LocalDateTime.now());
        this.review2 = CenterReview.of(4, "testContent2", this.user, this.center);
        ReflectionTestUtils.setField(this.review2, "id", "review2Id");
        ReflectionTestUtils.setField(this.review2, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(this.review2, "updatedAt", LocalDateTime.now());
    }

    @Test
    @DisplayName("Success case for create center review")
    void successCreateReview() {
        try (MockedStatic<CenterReview> reviewMockedStatic = mockStatic(CenterReview.class)) {
            // given
            ReviewCreateRequestDto reviewCreateRequestDto = new ReviewCreateRequestDto(5, "testContent");

            given(this.userRepository.findById("testUserId")).willReturn(Optional.of(user));
            given(this.centerRepository.findById("testCenterId")).willReturn(Optional.of(center));

            reviewMockedStatic.when(() -> CenterReview.of(5, "testContent", this.user, this.center)).thenReturn(this.review1);

            given(this.reviewRepository.selectRanksByCenterId("testCenterId")).willReturn(List.of());
            given(this.reviewRepository.save(this.review1)).willReturn(this.review1);

            // when
            ReviewResponseDto reviewResponseDto = this.centerReviewService.createReview("testUserId", "testCenterId", reviewCreateRequestDto);

            // then
            assertThat(reviewResponseDto)
                    .isNotNull()
                    .extracting("reviewId", "content")
                    .contains(this.review1.getId(), this.review1.getContent());
        }
    }

    @Test
    @DisplayName("Failure case for create center review for existing own review in center")
    void failureCreateReview_alreadyExist() {
        ReviewCreateRequestDto reviewCreateRequestDto = new ReviewCreateRequestDto(5, "testContent");

        given(this.userRepository.findById("userId")).willReturn(Optional.of(user));
        given(this.centerRepository.findById("center id")).willReturn(Optional.of(center));
        given(this.reviewRepository.findByUserIdAndCenterId(user.getId(), center.getId())).willReturn(Optional.of(review1));

        // when
        final BadRequestException ex = Assertions.assertThrows(
                BadRequestException.class,
                () -> this.centerReviewService.createReview("userId", "center id", reviewCreateRequestDto)
        );

        // then
        assertThat(ex)
                .extracting("errorCode", "message")
                .contains(ErrorCode.ROW_ALREADY_EXIST, "이미 작성된 리뷰가 존재합니다.");
    }

    @Test
    @DisplayName("Success case for update review")
    void successUpdateReview() {
        // given
        ReviewUpdateRequestDto reviewUpdateRequestDto = new ReviewUpdateRequestDto(1, "updateContent");

        given(this.userRepository.findById("userId")).willReturn(Optional.of(user));
        given(this.reviewRepository.findById("review1Id")).willReturn(Optional.of(review1));
        given(this.reviewRepository.selectRanksByCenterId("center id")).willReturn(List.of(2));
        given(this.reviewRepository.save(this.review1)).willReturn(this.review1);

        // when
        ReviewResponseDto reviewResponseDto = this.centerReviewService.updateReview("userId", "review1Id", reviewUpdateRequestDto);

        // then
        assertThat(reviewResponseDto)
                .isNotNull()
                .extracting("content", "reviewId")
                .contains("updateContent", "review1Id");
    }

    @Test
    @DisplayName("Failure case for update review because update by other user")
    void failUpdateReview_Unauthorized() {
        // given
        ReviewUpdateRequestDto reviewUpdateRequestDto = new ReviewUpdateRequestDto(1, "updateContent");

        given(this.userRepository.findById("userId")).willReturn(Optional.of(user2));
        given(this.reviewRepository.findById("reviewId")).willReturn(Optional.of(review1));

        // when
        final UnauthorizedException ex = Assertions.assertThrows(
                UnauthorizedException.class,
                () -> this.centerReviewService.updateReview("userId", "reviewId", reviewUpdateRequestDto)
        );

        // then
        assertThat(ex)
                .extracting("errorCode", "message")
                .contains(ErrorCode.NOT_ACCESSIBLE, "접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("Success case for delete review")
    void successDeleteReview() {
        // given
        given(this.userRepository.findById("userId")).willReturn(Optional.of(user));
        given(this.reviewRepository.findById("review1Id")).willReturn(Optional.of(review1));
        given(this.reviewRepository.selectRanksByCenterId("center id")).willReturn(List.of(2));

        // when
        this.centerReviewService.deleteReview("userId", "review1Id");

        // then
        assertThat(this.reviewRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Failure case for delete review because update by other user")
    void failDeleteReview_Unauthorized() {
        // given
        given(this.userRepository.findById("userId")).willReturn(Optional.of(user2));
        given(this.reviewRepository.findById("reviewId")).willReturn(Optional.of(review1));

        // when
        final UnauthorizedException ex = Assertions.assertThrows(
                UnauthorizedException.class,
                () -> this.centerReviewService.deleteReview("userId", "reviewId")
        );

        // then
        assertThat(ex)
                .extracting("errorCode", "message")
                .contains(ErrorCode.NOT_ACCESSIBLE, "접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("Success case for find center review")
    void successFindReview() {
        //given
        Pageable pageable = PageRequest.of(0, 2);
        Page<CenterReview> centerReviewPage = new PageImpl<>(List.of(review1, review2), pageable, 2);

        given(this.userRepository.findById("userId")).willReturn(Optional.of(user));

        given(this.centerRepository.findById("centerId")).willReturn(Optional.of(center));
        given(this.reviewRepository.selectRanksByCenterId("centerId")).willReturn(List.of(5, 4));
        given(this.reviewRepositorySupport.findByCenterExceptBlockUser(center.getId(), "userId", pageable)).willReturn(centerReviewPage);

        //when
        ReviewListFindResponseDto reviewListFindResponseDto = this.centerReviewService.findReview("userId", "centerId", pageable);

        // then
        assertThat(reviewListFindResponseDto.getReviewFindResponseDtoPagination().getResults())
                .isNotNull()
                .extracting(ReviewFindResponseDto::getReviewId, ReviewFindResponseDto::getRank)
                .contains(
                        tuple(review1.getId(), review1.getRank()),
                        tuple(review2.getId(), review2.getRank())
                );
    }
}