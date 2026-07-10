package com.example.kiosksim.service;

import com.example.kiosksim.domain.Member;
import com.example.kiosksim.domain.PointHistory;
import com.example.kiosksim.domain.KioskOrder;
import com.example.kiosksim.repository.MemberRepository;
import com.example.kiosksim.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {
    
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 회원 조회 또는 생성
    public Member getOrCreateMember(String phoneNumber) {
        return memberRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .phoneNumber(phoneNumber)
                            .totalPoints(0)
                            .availablePoints(0)
                            .build();
                    return memberRepository.save(newMember);
                });
    }

    // 회원 조회
    public Member getMemberByPhoneNumber(String phoneNumber) {
        return memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("회원이 없습니다: " + phoneNumber));
    }

    // 포인트 적립 (주문 완료 시)
    public void earnPoints(Member member, Integer orderAmount, KioskOrder order) {
        // 주문 금액의 1% 적립
        Integer earnedPoints = (int) (orderAmount * 0.01);
        
        member.addPoints(earnedPoints);
        memberRepository.save(member);

        // 포인트 히스토리 기록
        PointHistory history = PointHistory.builder()
                .member(member)
                .order(order)
                .pointAmount(earnedPoints)
                .type("EARNED")
                .reason("Order Payment")
                .balanceAfter(member.getAvailablePoints())
                .build();
        pointHistoryRepository.save(history);
    }

    // 포인트 사용
    public boolean usePoints(Member member, Integer points, KioskOrder order) {
        if (!member.usePoints(points)) {
            return false;  // 포인트 부족
        }

        memberRepository.save(member);

        // 포인트 히스토리 기록
        PointHistory history = PointHistory.builder()
                .member(member)
                .order(order)
                .pointAmount(-points)
                .type("USED")
                .reason("Point Usage")
                .balanceAfter(member.getAvailablePoints())
                .build();
        pointHistoryRepository.save(history);

        return true;
    }

    // 포인트 조회
    public Integer getAvailablePoints(String phoneNumber) {
        Member member = getMemberByPhoneNumber(phoneNumber);
        return member.getAvailablePoints();
    }

    // 회원 등록 또는 포인트 수정
    public Member saveOrUpdateMember(String phoneNumber, Integer points) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> Member.builder()
                        .phoneNumber(phoneNumber)
                        .totalPoints(0)
                        .availablePoints(0)
                        .build());
        member.setAvailablePoints(points);
        if (member.getTotalPoints() < points) {
            member.setTotalPoints(points);
        }
        return memberRepository.save(member);
    }
}
