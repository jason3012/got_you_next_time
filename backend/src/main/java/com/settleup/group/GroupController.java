package com.settleup.group;

import com.settleup.auth.AuthenticatedUser;
import com.settleup.group.dto.AddGroupMemberRequest;
import com.settleup.group.dto.CreateGroupRequest;
import com.settleup.group.dto.GroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.create(AuthenticatedUser.id(jwt), request));
    }

    @GetMapping
    public List<GroupResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return groupService.listForUser(AuthenticatedUser.id(jwt));
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable UUID groupId, @AuthenticationPrincipal Jwt jwt) {
        return groupService.get(groupId, AuthenticatedUser.id(jwt));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupService.addMember(groupId, AuthenticatedUser.id(jwt), request));
    }

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberUserId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        groupService.removeMember(groupId, AuthenticatedUser.id(jwt), memberUserId);
        return ResponseEntity.noContent().build();
    }
}
