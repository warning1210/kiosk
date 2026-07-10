package com.example.kiosksim.controller;

import com.example.kiosksim.domain.Member;
import com.example.kiosksim.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/points")
    public ResponseEntity<?> getPoints(@RequestParam String phoneNumber) {
        try {
            Member member = memberService.getMemberByPhoneNumber(phoneNumber);
            return ResponseEntity.ok(Map.of(
                "phoneNumber", member.getPhoneNumber(),
                "availablePoints", member.getAvailablePoints()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> saveOrUpdateMember(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = (String) request.get("phoneNumber").toString();
            Integer points = Integer.valueOf(request.get("points").toString());
            Member member = memberService.saveOrUpdateMember(phoneNumber, points);
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
