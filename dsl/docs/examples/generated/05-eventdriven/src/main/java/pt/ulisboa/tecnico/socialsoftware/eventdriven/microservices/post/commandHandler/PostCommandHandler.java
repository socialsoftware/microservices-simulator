package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.post.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.service.PostService;

import java.util.logging.Logger;

@Component
public class PostCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(PostCommandHandler.class.getName());

    @Autowired
    private PostService postService;

    @Override
    protected String getAggregateTypeName() {
        return "Post";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreatePostCommand cmd -> handleCreatePost(cmd);
            case GetPostByIdCommand cmd -> handleGetPostById(cmd);
            case GetAllPostsCommand cmd -> handleGetAllPosts(cmd);
            case UpdatePostCommand cmd -> handleUpdatePost(cmd);
            case DeletePostCommand cmd -> handleDeletePost(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreatePost(CreatePostCommand cmd) {
        logger.info("handleCreatePost");
        try {
            return postService.createPost(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetPostById(GetPostByIdCommand cmd) {
        logger.info("handleGetPostById");
        try {
            return postService.getPostById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllPosts(GetAllPostsCommand cmd) {
        logger.info("handleGetAllPosts");
        try {
            return postService.getAllPosts(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdatePost(UpdatePostCommand cmd) {
        logger.info("handleUpdatePost");
        try {
            return postService.updatePost(cmd.getPostDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeletePost(DeletePostCommand cmd) {
        logger.info("handleDeletePost");
        try {
            postService.deletePost(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
