package com.kiosk.application;

import com.kiosk.domain.admin.*;
import com.kiosk.domain.branch.*;
import com.kiosk.domain.branchapplication.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BranchAuthController {
    private final BranchApplicationRepository applications;
    private final BranchRepository branches;
    private final AdminRepository admins;
    public BranchAuthController(BranchApplicationRepository applications, BranchRepository branches, AdminRepository admins) {
        this.applications=applications;this.branches=branches;this.admins=admins;
    }

    @PostMapping("/branch-auth/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApplicationResponse apply(@RequestBody ApplyRequest request) {
        if(request.loginId()==null||request.loginId().trim().length()<4)throw new IllegalArgumentException("아이디는 4자 이상 입력하세요.");
        if(request.password()==null||request.password().length()<8)throw new IllegalArgumentException("비밀번호는 8자 이상 입력하세요.");
        if(admins.existsByLoginId(request.loginId().trim()))throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if(admins.findByEmail(request.email()).isPresent())throw new IllegalArgumentException("이미 신청되었거나 사용 중인 이메일입니다.");
        UserRecord firebaseUser;
        try {
            firebaseUser=FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(request.email()).setPassword(request.password()).setDisplayName(request.managerName())
                    .setEmailVerified(true).setDisabled(true));
        } catch (Exception e) {
            throw new IllegalArgumentException("계정 신청 정보를 등록할 수 없습니다: "+e.getMessage());
        }
        admins.save(Admin.builder().loginId(request.loginId().trim()).passwordHash("FIREBASE$"+firebaseUser.getUid())
                .name(request.managerName()).phone(request.phone()).email(request.email())
                .role(AdminRole.BRANCH_MANAGER).accountStatus(AccountStatus.PENDING).build());
        BranchApplication saved=applications.save(BranchApplication.builder().branchName(request.branchName())
                .managerName(request.managerName()).phone(request.phone()).email(request.email()).address(request.address())
                .businessNumber(request.businessNumber()).approvalStatus(ApprovalStatus.PENDING).appliedAt(LocalDateTime.now()).build());
        return response(saved,null);
    }

    @GetMapping("/hq/branch-applications")
    @Transactional(readOnly=true)
    public List<ApplicationResponse> applications(@RequestHeader(value="Origin",required=false)String origin){
        String base=origin==null?"http://localhost:5173":origin;
        return applications.findAllByOrderByAppliedAtDesc().stream()
                .map(a->response(a,a.getInviteToken()==null?null:base+"/branch/join?token="+a.getInviteToken())).toList();
    }

    @PostMapping("/hq/branch-applications/{id}/approve")
    @Transactional
    public ApplicationResponse approve(@PathVariable Long id,@RequestHeader(value="Origin",required=false)String origin){
        BranchApplication application=applications.findById(id).orElseThrow();
        if(application.getApprovalStatus()!=ApprovalStatus.PENDING)throw new IllegalStateException("이미 처리된 신청입니다.");
        Admin admin=admins.findByEmail(application.getEmail())
                .filter(a->a.getRole()==AdminRole.BRANCH_MANAGER&&a.getAccountStatus()==AccountStatus.PENDING)
                .orElseThrow(()->new IllegalStateException("이 신청에는 로그인 계정 정보가 없습니다. 지점에서 다시 신청해야 합니다."));
        Branch branch=branches.save(Branch.builder().branchName(application.getBranchName()).address(application.getAddress())
                .phone(application.getPhone()).email(application.getEmail()).managerName(application.getManagerName())
                .operationStatus(OperationStatus.PENDING).kioskStatus(KioskStatus.ACTIVE).build());
        application.setApprovalStatus(ApprovalStatus.APPROVED);application.setApprovedBranch(branch);
        application.setInviteToken(null);application.setInviteExpiresAt(null);application.setProcessedAt(LocalDateTime.now());
        applications.save(application);
        admin.setBranch(branch);admin.setAccountStatus(AccountStatus.ACTIVE);admins.save(admin);
        try {
            String uid=admin.getPasswordHash().substring("FIREBASE$".length());
            FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(uid).setDisabled(false));
            FirebaseAuth.getInstance().setCustomUserClaims(uid,Map.of("role","BRANCH_MANAGER","branchId",branch.getBranchId()));
        } catch (Exception e) {
            throw new IllegalStateException("Firebase 계정을 활성화하지 못했습니다: "+e.getMessage());
        }
        branch.setOperationStatus(OperationStatus.ACTIVE);branches.save(branch);
        return response(application,null);
    }

    @DeleteMapping("/hq/branch-applications/{id}/account")
    @Transactional
    public ApplicationResponse deleteBranchAccount(@PathVariable Long id){
        BranchApplication application=applications.findById(id).orElseThrow();
        if(application.getApprovalStatus()!=ApprovalStatus.APPROVED)throw new IllegalStateException("수락 완료된 지점 계정만 삭제할 수 있습니다.");
        Admin admin=admins.findByEmail(application.getEmail())
                .filter(a->a.getRole()==AdminRole.BRANCH_MANAGER)
                .orElseThrow(()->new IllegalStateException("삭제할 지점 계정을 찾을 수 없습니다."));
        if(admin.getAccountStatus()==AccountStatus.DELETED)return response(application,null);
        try {
            UserRecord firebaseUser=FirebaseAuth.getInstance().getUserByEmail(admin.getEmail());
            FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(firebaseUser.getUid()).setDisabled(true));
            FirebaseAuth.getInstance().revokeRefreshTokens(firebaseUser.getUid());
        } catch (Exception e) {
            throw new IllegalStateException("Firebase 계정을 삭제 상태로 변경하지 못했습니다: "+e.getMessage());
        }
        admin.setAccountStatus(AccountStatus.DELETED);
        admins.save(admin);
        return response(application,null);
    }

    @GetMapping("/branch-auth/invites/{token}")
    @Transactional(readOnly=true)
    public ApplicationResponse invite(@PathVariable String token){return response(validInvite(token),null);}

    @PostMapping("/branch-auth/join")
    @Transactional
    public LoginResponse join(@RequestBody JoinRequest request){
        BranchApplication application=validInvite(request.token());
        if(admins.existsByLoginId(request.loginId()))throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        Branch branch=application.getApprovedBranch();
        UserRecord firebaseUser;
        try {
            firebaseUser=FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(application.getEmail()).setPassword(request.password()).setDisplayName(application.getManagerName()).setEmailVerified(true));
            FirebaseAuth.getInstance().setCustomUserClaims(firebaseUser.getUid(),Map.of("role","BRANCH_MANAGER","branchId",branch.getBranchId()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Firebase 계정을 만들 수 없습니다: "+e.getMessage());
        }
        Admin admin=admins.save(Admin.builder().branch(branch).loginId(request.loginId()).passwordHash("FIREBASE$"+firebaseUser.getUid())
                .name(application.getManagerName()).phone(application.getPhone()).email(application.getEmail())
                .role(AdminRole.BRANCH_MANAGER).accountStatus(AccountStatus.ACTIVE).build());
        branch.setOperationStatus(OperationStatus.ACTIVE);branches.save(branch);
        application.setInviteToken(null);applications.save(application);
        return new LoginResponse(admin.getAdminId(),branch.getBranchId(),branch.getBranchName(),admin.getName());
    }

    @GetMapping("/branch-auth/login-identity/{loginId}")
    public LoginIdentity loginIdentity(@PathVariable String loginId){
        Admin admin=admins.findByLoginId(loginId).orElseThrow(()->new IllegalArgumentException("등록되지 않은 아이디입니다."));
        if(admin.getRole()!=AdminRole.BRANCH_MANAGER||admin.getAccountStatus()!=AccountStatus.ACTIVE)throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
        return new LoginIdentity(admin.getEmail());
    }

    @PostMapping("/branch-auth/firebase-session")
    @Transactional
    public LoginResponse firebaseSession(@RequestBody FirebaseSessionRequest request){
        try {
            FirebaseToken token=FirebaseAuth.getInstance().verifyIdToken(request.idToken(),true);
            Admin admin=admins.findByEmail(token.getEmail()).orElseThrow(()->new IllegalArgumentException("승인된 지점 계정이 아닙니다."));
            if(admin.getRole()!=AdminRole.BRANCH_MANAGER||admin.getAccountStatus()!=AccountStatus.ACTIVE)throw new IllegalArgumentException("사용할 수 없는 계정입니다.");
            admin.setLastLoginAt(LocalDateTime.now());admins.save(admin);
            return new LoginResponse(admin.getAdminId(),admin.getBranch().getBranchId(),admin.getBranch().getBranchName(),admin.getName());
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalArgumentException("Firebase 로그인 토큰이 유효하지 않습니다."); }
    }

    @PostMapping("/branch-auth/login")
    @Transactional
    public LoginResponse login(@RequestBody LoginRequest request){
        Admin admin=admins.findByLoginId(request.loginId()).orElseThrow(()->new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));
        if(!verify(request.password(),admin.getPasswordHash())||admin.getRole()!=AdminRole.BRANCH_MANAGER||admin.getAccountStatus()!=AccountStatus.ACTIVE)
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        admin.setLastLoginAt(LocalDateTime.now());admins.save(admin);
        return new LoginResponse(admin.getAdminId(),admin.getBranch().getBranchId(),admin.getBranch().getBranchName(),admin.getName());
    }

    private BranchApplication validInvite(String token){
        BranchApplication application=applications.findByInviteToken(token).orElseThrow(()->new IllegalArgumentException("유효하지 않은 초대 URL입니다."));
        if(application.getInviteExpiresAt()==null||application.getInviteExpiresAt().isBefore(LocalDateTime.now()))throw new IllegalArgumentException("초대 URL이 만료되었습니다.");
        return application;
    }
    private ApplicationResponse response(BranchApplication a,String inviteUrl){
        String loginId=admins.findByEmail(a.getEmail()).map(Admin::getLoginId).orElse(null);
        String accountStatus=admins.findByEmail(a.getEmail()).map(admin->admin.getAccountStatus().name()).orElse(null);
        return new ApplicationResponse(a.getBranchApplicationId(),a.getManagerName(),a.getBranchName(),a.getAddress(),a.getEmail(),a.getPhone(),a.getBusinessNumber(),loginId,accountStatus,a.getApprovalStatus().name(),a.getAppliedAt(),inviteUrl);
    }
    private String hash(String password){
        try{byte[]salt=new byte[16];new SecureRandom().nextBytes(salt);PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,120000,256);byte[]key=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return"pbkdf2$120000$"+Base64.getEncoder().encodeToString(salt)+"$"+Base64.getEncoder().encodeToString(key);}catch(Exception e){throw new IllegalStateException(e);}
    }
    private boolean verify(String password,String encoded){
        try{String[]p=encoded.split("\\$");if(p.length!=4||!"pbkdf2".equals(p[0]))return false;byte[]salt=Base64.getDecoder().decode(p[2]);PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,Integer.parseInt(p[1]),256);byte[]actual=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();return MessageDigest.isEqual(actual,Base64.getDecoder().decode(p[3]));}catch(Exception e){return false;}
    }

    public record ApplyRequest(String managerName,String branchName,String address,String email,String phone,String businessNumber,String loginId,String password){}
    public record JoinRequest(String token,String loginId,String password){}
    public record LoginRequest(String loginId,String password){}
    public record LoginIdentity(String email){}
    public record FirebaseSessionRequest(String idToken){}
    public record LoginResponse(Long adminId,Long branchId,String branchName,String managerName){}
    public record ApplicationResponse(Long applicationId,String managerName,String branchName,String address,String email,String phone,String businessNumber,String loginId,String accountStatus,String status,LocalDateTime appliedAt,String inviteUrl){}
}
