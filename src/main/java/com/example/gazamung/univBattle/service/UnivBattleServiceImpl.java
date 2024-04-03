package com.example.gazamung.univBattle.service;

import com.example.gazamung.ChatRoom.ChatRoom;
import com.example.gazamung.ChatRoom.ChatRoomRepository;
import com.example.gazamung._enum.CustomExceptionCode;
import com.example.gazamung._enum.Status;
import com.example.gazamung.exception.CustomException;
import com.example.gazamung.member.entity.Member;
import com.example.gazamung.member.repository.MemberRepository;
import com.example.gazamung.participant.entity.Participant;
import com.example.gazamung.participant.repository.ParticipantRepository;
import com.example.gazamung.univBattle.dto.AttendRequest;
import com.example.gazamung.univBattle.dto.GuestLeaderAttendRequest;
import com.example.gazamung.univBattle.dto.UnivBattleCreateRequest;
import com.example.gazamung.univBattle.entity.UnivBattle;
import com.example.gazamung.univBattle.repository.UnivBattleRepository;
import com.example.gazamung.university.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnivBattleServiceImpl implements UnivBattleService {

    private final UniversityRepository universityRepository;
    private final UnivBattleRepository univBattleRepository;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ParticipantRepository participantRepository;


    /**
     * 대항전 생성
     * @param request
     * @return
     */
    @Override
    public boolean create(UnivBattleCreateRequest request) {

        Member member = memberRepository.findById(request.getHostLeader())
                .orElseThrow(() -> new CustomException(CustomExceptionCode.NOT_FOUND_USER));

        long univId = member.getUnivId();

        // 대항전 생성
        UnivBattle univBattle = UnivBattle.builder()
                .hostLeader(request.getHostLeader())
                .eventId(request.getEventId())
                .hostUniv(univId)
                .battleDate(request.getBattleDate())
                .location(request.getLocation())
                .content(request.getContent())
                .totalParticipants(request.getTotalParticipants())
                .status(Status.WAITING)
                .cost(request.getCost())
                .regDt(LocalDateTime.now())
                .build();

        UnivBattle result = univBattleRepository.save(univBattle);

        //대항전 참가자 테이블에 생성자 추가
        Participant participant = Participant.builder()
                .memberIdx(member.getMemberIdx())
                .univBattleId(result.getUnivBattleId())
                .univId(univId)
                .build();
        participantRepository.save(participant);

        // 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .chatRoomType(0)
                .univBattleId(univBattle.getUnivBattleId())
                .build();

        chatRoomRepository.save(chatRoom);

        return true;

    }

    /**
     *  대항전 대표 참가
     * @param request
     * @return
     */
    @Override
    public boolean GuestLeaderAttend(GuestLeaderAttendRequest request) {

        UnivBattle univBattle = univBattleRepository.findById(request.getUnivBattleId())
                .orElseThrow(() -> new CustomException(CustomExceptionCode.NOT_FOUND_BATTLE));

        // 참가자 정보 조회
        Member guest = memberRepository.findById(request.getGuestLeader())
                        .orElseThrow(() -> new CustomException(CustomExceptionCode.NOT_FOUND_USER));

        // 주최자 대학교
        long hostUniv = univBattle.getHostUniv();
        // 참가자 대학교
        long guestUniv = guest.getUnivId();

        // 같은 학교는 참가 불가능.
        if (hostUniv == guestUniv) {
            throw new CustomException(CustomExceptionCode.SAME_UNIVERSITY);
        }

        univBattle.setGuestLeader(request.getGuestLeader());
        univBattle.setGuestUniv(guestUniv);

        // 초대코드 생성
        univBattle.setInvitationCode(generateRandomString(8));

        univBattleRepository.save(univBattle);

        Participant participant = Participant.builder()
                .memberIdx(guest.getMemberIdx())
                .univBattleId(request.getUnivBattleId())
                .univId(guestUniv)
                .build();

        participantRepository.save(participant);

        return true;

    }

    @Override
    public boolean attend(AttendRequest request) {

        // 참가자 정보 조회
        Member member = memberRepository.findById(request.getMemberIdx())
                .orElseThrow(()-> new CustomException(CustomExceptionCode.NOT_FOUND_USER));

        // 대항전 정보 조회
        UnivBattle univBattle = univBattleRepository.findById(request.getUnivBattleId())
                .orElseThrow(() -> new CustomException(CustomExceptionCode.NOT_FOUND_BATTLE));

        // 이미 참가한 경우 예외 처리
        boolean alreadyAttended = participantRepository.existsByMemberIdxAndUnivBattleId(member.getMemberIdx(), univBattle.getUnivBattleId());
        if (alreadyAttended) {
            throw new CustomException(CustomExceptionCode.ALREADY_ATTENDED);
        }

        // 참가인원 초과 여부 체크
        int totalParticipant = participantRepository.countByUnivBattleId(univBattle.getUnivBattleId());
        if (totalParticipant >= univBattle.getTotalParticipants()) {
            throw new CustomException(CustomExceptionCode.EXCEEDED_TOTAL_CAPACITY);
        }

        // 대학별 인원 초과 여부 체크
        int univTotalParticipant = participantRepository.countByUnivBattleIdAndUnivId(univBattle.getUnivBattleId(), member.getUnivId());
        if (univTotalParticipant >= univBattle.getTotalParticipants() / 2) {
            throw new CustomException(CustomExceptionCode.EXCEEDED_UNIV_CAPACITY);
        }

        // 참가 코드 체크
        if (!request.getInvitationCode().equals(univBattle.getInvitationCode())){
            throw new CustomException(CustomExceptionCode.INVALID_INVITE_CODE);
        }

        // 참가자 저장
        Participant participant = Participant.builder()
                .memberIdx(request.getMemberIdx())
                .univBattleId(request.getUnivBattleId())
                .univId(member.getUnivId())
                .build();
        participantRepository.save(participant);

        return true;
    }


    /**
     * 초대 코드 생성기
     * @author 이시영
     * @param length
     * @return
     */
    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().encodeToString(bytes).substring(0, length);
    }
}
