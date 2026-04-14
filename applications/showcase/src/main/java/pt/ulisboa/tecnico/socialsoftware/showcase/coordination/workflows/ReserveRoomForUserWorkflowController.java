package pt.ulisboa.tecnico.socialsoftware.showcase.coordination.workflows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReserveRoomForUserWorkflowController {

    @Autowired private ReserveRoomForUserWorkflow reserveRoomForUserWorkflow;

    @PostMapping("/workflows/ReserveRoomForUser")
    public void run(@RequestBody ReserveRoomForUserWorkflowRequestDto request) {
        reserveRoomForUserWorkflow.execute(request.getUsername(), request.getEmail(), request.getRoomId(), request.getCheckIn(), request.getCheckOut(), request.getNights(), request.getPrice());
    }
}
