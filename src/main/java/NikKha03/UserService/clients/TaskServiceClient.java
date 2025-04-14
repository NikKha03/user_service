package NikKha03.UserService.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "TASKSERVICE")
public interface TaskServiceClient {

    @GetMapping("/task_service/projectOwner/user/{username}")
    boolean isDataHave(@PathVariable String username);

    @PostMapping("/task_service/projectOwner/user")
    void pushUserInTaskService(@RequestBody Map<String, String> user);

}
