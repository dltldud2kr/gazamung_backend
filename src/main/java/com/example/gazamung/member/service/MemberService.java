package com.example.gazamung.member.service;

import com.example.gazamung.dto.TokenDto;
import com.example.gazamung.member.dto.JoinRequestDto;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public interface MemberService {

    /**
     * 회원가입
     */
    TokenDto join(JoinRequestDto dto);

    /**
     * 카카오 회원가입
     */
    TokenDto join(String email, String kakaoIdx, String nickname);

    /**
     * 카카오 로그인
     */
    TokenDto kakaoLogin(String email, String password);

    /**
     * 회원 로그인
     * @param email
     * @param password
     * @return
     */
    TokenDto login(String email, String password);


    /**
     * 토큰 생성
     * @param memberIdx
     * @return
     */
    TokenDto createToken(String memberIdx);

    /**
     * 이메일 인증확인 (발송된 인증번호와 동일한지 확인)
     * @param email
     * @param verifCode
     * @return
     */
    boolean emailCheck(String email, String verifCode);



    /**
     * 비밀번호 변경
     */

    boolean changePw(String email, String password);

    /**
     * 회원인지 확인
     * @param email
     * @return
     */
    boolean isMember(String email);

    /**
     *
     * 카카오 로그인 토큰 값
     */
    String getReturnAccessToken(String code, HttpServletRequest request);

    /**
     * 카카오로그인 파싱 결과
     * @param access_token
     * @return
     */
    public Map<String,Object> getUserInfo(String access_token);

}
