package com.collab.service;

import com.collab.common.Result;
import com.collab.dto.MemberDTO;

public interface DocumentMemberService {

    Result<?> addMember(Long docId, MemberDTO dto, Long operatorId);

    Result<?> removeMember(Long docId, Long userId, Long operatorId);

    Result<?> updatePermission(Long docId, MemberDTO dto, Long operatorId);

    Result<?> listMembers(Long docId);
}
