package com.company.fashion.modules.match.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

  private final Map<String, List<SseEmitter>> emittersByTaskId = new ConcurrentHashMap<>();

  public SseEmitter subscribe(String taskId) {
    SseEmitter emitter = new SseEmitter(0L);
    emittersByTaskId.computeIfAbsent(taskId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

    emitter.onCompletion(() -> remove(taskId, emitter));
    emitter.onTimeout(() -> remove(taskId, emitter));
    emitter.onError(ex -> remove(taskId, emitter));
    return emitter;
  }

  public void publish(String taskId, String eventName, Object payload) {
    List<SseEmitter> emitters = emittersByTaskId.get(taskId);
    if (emitters == null || emitters.isEmpty()) {
      return;
    }

    emitters.removeIf(emitter -> !send(emitter, taskId, eventName, payload));
    if ("task_completed".equals(eventName) || "task_failed".equals(eventName)) {
      emitters.forEach(SseEmitter::complete);
      emittersByTaskId.remove(taskId);
    }
  }

  private boolean send(SseEmitter emitter, String taskId, String eventName, Object payload) {
    try {
      emitter.send(SseEmitter.event().name(eventName).id(taskId).data(payload));
      return true;
    } catch (IOException ex) {
      emitter.complete();
      return false;
    }
  }

  private void remove(String taskId, SseEmitter emitter) {
    List<SseEmitter> emitters = emittersByTaskId.get(taskId);
    if (emitters == null) {
      return;
    }

    emitters.remove(emitter);
    if (emitters.isEmpty()) {
      emittersByTaskId.remove(taskId);
    }
  }
}
