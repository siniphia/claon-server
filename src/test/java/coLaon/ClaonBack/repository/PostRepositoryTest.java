package coLaon.ClaonBack.repository;

import coLaon.ClaonBack.center.domain.*;
import coLaon.ClaonBack.center.repository.CenterRepository;
import coLaon.ClaonBack.post.domain.Post;
import coLaon.ClaonBack.post.repository.PostRepository;
import coLaon.ClaonBack.user.domain.User;
import coLaon.ClaonBack.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class PostRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private PostRepository postRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUp(){
        // given
        this.user = User.of(
                "test@gmail.com",
                "1234567890",
                "test",
                "경기도",
                "성남시",
                "",
                "",
                "instagramId"
        );
        userRepository.save(user);
        Center center = Center.of(
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
        centerRepository.save(center);
        this.post = Post.of(
                center,
                "testContent1",
                user,
                Set.of(),
                Set.of()
        );
        postRepository.save(post);
    }

    @Test
    public void checkSelectPostIdsByUserId(){
        // when
        List<String> postIds = postRepository.selectPostIdsByUserId(user.getId());
        // then
        assertThat(postIds.contains(post.getId())).isTrue();
    }
}