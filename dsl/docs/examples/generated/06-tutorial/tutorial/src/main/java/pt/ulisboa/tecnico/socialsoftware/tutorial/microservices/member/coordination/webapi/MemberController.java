package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.functionalities.MemberFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.webapi.requestDtos.CreateMemberRequestDto;

@RestController
public class MemberController {
    @Autowired
    private MemberFunctionalities memberFunctionalities;

    @PostMapping("/members/create")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDto createMember(@RequestBody CreateMemberRequestDto createRequest) {
        return memberFunctionalities.createMember(createRequest);
    }

    @GetMapping("/members/{memberAggregateId}")
    public MemberDto getMemberById(@PathVariable Integer memberAggregateId) {
        return memberFunctionalities.getMemberById(memberAggregateId);
    }

    @PutMapping("/members")
    public MemberDto updateMember(@RequestBody MemberDto memberDto) {
        return memberFunctionalities.updateMember(memberDto);
    }

    @DeleteMapping("/members/{memberAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(@PathVariable Integer memberAggregateId) {
        memberFunctionalities.deleteMember(memberAggregateId);
    }

    @GetMapping("/members")
    public List<MemberDto> getAllMembers() {
        return memberFunctionalities.getAllMembers();
    }
}
