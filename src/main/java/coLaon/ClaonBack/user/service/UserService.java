package coLaon.ClaonBack.user.service;

import coLaon.ClaonBack.common.domain.enums.BasicLocalArea;
import coLaon.ClaonBack.common.domain.enums.MetropolitanArea;
import coLaon.ClaonBack.common.exception.BadRequestException;
import coLaon.ClaonBack.common.exception.ErrorCode;
import coLaon.ClaonBack.common.exception.UnauthorizedException;
import coLaon.ClaonBack.common.utils.JwtUtil;
import coLaon.ClaonBack.config.dto.JwtDto;
import coLaon.ClaonBack.user.domain.OAuth2Provider;
import coLaon.ClaonBack.user.domain.User;
import coLaon.ClaonBack.user.dto.DuplicatedCheckResponseDto;
import coLaon.ClaonBack.user.dto.OAuth2UserInfoDto;
import coLaon.ClaonBack.user.dto.SignInRequestDto;
import coLaon.ClaonBack.user.dto.SignUpRequestDto;
import coLaon.ClaonBack.user.dto.UserResponseDto;
import coLaon.ClaonBack.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OAuth2UserInfoProviderSupplier oAuth2UserInfoProviderSupplier;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public DuplicatedCheckResponseDto nicknameDuplicatedCheck(String nickname) {
        return DuplicatedCheckResponseDto.of(this.userRepository.findByNickname(nickname).isPresent());
    }

    @Transactional
    public JwtDto signIn(
            String provider,
            SignInRequestDto signInRequestDto
    ) {
        OAuth2UserInfoDto userInfoDto = this.oAuth2UserInfoProviderSupplier.getProvider(OAuth2Provider.of(provider))
                .getUserInfo(signInRequestDto.getCode());

        User user = this.userRepository.findByEmailAndOAuthId(userInfoDto.getEmail(), userInfoDto.getOAuthId())
                .orElseGet(() -> this.userRepository.save(User.of(userInfoDto.getEmail(), userInfoDto.getOAuthId())));

        return this.jwtUtil.createToken(
                user.getId(),
                Optional.ofNullable(user.getNickname()).isPresent()
        );
    }

    @Transactional
    public UserResponseDto signUp(
            String userId,
            SignUpRequestDto signUpRequestDto
    ) {
        User user = this.userRepository.findById(userId).orElseThrow(
                () -> {
                    throw new UnauthorizedException(
                            ErrorCode.USER_DOES_NOT_EXIST,
                            "이용자를 찾을 수 없습니다."
                    );
                });

        this.userRepository.findByNickname(signUpRequestDto.getNickname()).ifPresent(
                u -> {
                    throw new BadRequestException(
                            ErrorCode.ROW_ALREADY_EXIST,
                            "이미 존재하는 닉네임입니다."
                    );
                }
        );

        this.userRepository.findByInstagramOAuthId(signUpRequestDto.getInstagramOAuthId()).ifPresent(
                u -> {
                    throw new BadRequestException(
                            ErrorCode.ROW_ALREADY_EXIST,
                            "이미 가입한 인스타그램 계정입니다."
                    );
                }
        );

        user.signUp(
                signUpRequestDto.getNickname(),
                MetropolitanArea.of(signUpRequestDto.getMetropolitanActiveArea()),
                BasicLocalArea.of(
                        signUpRequestDto.getMetropolitanActiveArea(),
                        signUpRequestDto.getBasicLocalActiveArea()
                ),
                signUpRequestDto.getImagePath(),
                signUpRequestDto.getInstagramOAuthId(),
                signUpRequestDto.getInstagramUserName()
        );

        return UserResponseDto.from(userRepository.save(user));
    }
}

