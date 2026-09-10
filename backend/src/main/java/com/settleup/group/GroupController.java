package com.settleup.group;

import com.settleup.group.dto.AddGroupMemberRequest;
import com.settleup.group.dto.CreateGroupRequest;
import com.settleup.group.dto.GroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            @RequestParam UUID userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(userId, request));
    }

    @GetMapping
    public List<GroupResponse> list(@RequestParam UUID userId) {
        return groupService.listForUser(userId);
    }

    @GetMapping("/{groupId}")
    public GroupResponse get(@PathVariable UUID groupId, @RequestParam UUID userId) {
        return groupService.get(groupId, userId);
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupResponse> addMember(
            @PathVariable UUID groupId,
            @RequestParam UUID userId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.addMember(groupId, userId, request));
    }

    @DeleteMapping("/{groupId}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberUserId,
            @RequestParam UUID userId
    ) {
        groupService.removeMember(groupId, userId, memberUserId);
        return ResponseEntity.noContent().build();
    }
}
