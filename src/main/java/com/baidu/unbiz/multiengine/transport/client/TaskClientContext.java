package com.baidu.unbiz.multiengine.transport.client;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.springframework.util.Assert;
import com.baidu.unbiz.multiengine.transport.dto.Signal;
import com.baidu.unbiz.multitask.log.AopLogFactory;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * Created by wangchongjie on 16/4/11.
 */
public class TaskClientContext {
    private static final Logger LOG = AopLogFactory.getLogger(TaskClientContext.class);

    public static ConcurrentHashMap<String, Channel> sessionChannelMap = new ConcurrentHashMap<String, Channel>();
    public static ConcurrentHashMap<String, TaskClient> sessionClientMap = new ConcurrentHashMap<String, TaskClient>();
    public static final AttributeKey<String> SESSION_ATTRIBUTE = AttributeKey.newInstance("SESSION_ATTRIBUTE");

    private static ConcurrentHashMap<String, ConcurrentHashMap<Long, SendFuture>> sessionResultMap =
            new ConcurrentHashMap<String, ConcurrentHashMap<Long, SendFuture>>();

    public static void placeSessionResult(String sessionKey, Long seqId, SendFuture futrue) {
        ConcurrentHashMap<Long, SendFuture> resultMap = sessionResultMap.get(sessionKey);
        if (resultMap == null) {
            resultMap = new ConcurrentHashMap<Long, SendFuture>();
            sessionResultMap.putIfAbsent(sessionKey, resultMap);
            resultMap = sessionResultMap.get(sessionKey);
        }
        resultMap.put(seqId, futrue);
    }

    public static void fillSessionResult(String sessionKey, Signal signal) {
        fillSessionResult(sessionKey, signal.getSeqId(), signal.getMessage());
    }

    public static void fillSessionResult(String sessionKey, Long seqId, Object result) {
        ConcurrentHashMap<Long, SendFuture> resultMap = sessionResultMap.get(sessionKey);
        Assert.notNull(resultMap);
        SendFuture futrue = resultMap.get(seqId);
        if (futrue == null) {
            LOG.error("future is null," + result.toString());
        }
        futrue.set(result);
    }

    public static void appendSessionResult(String sessionKey, Long seqId, Object result,
                                           SimpleSendFutrue.AppendHandler handler, boolean finished) {
        ConcurrentHashMap<Long, SendFuture> resultMap = sessionResultMap.get(sessionKey);

        SendFuture future = resultMap.get(seqId);
        future.append(result, handler, finished);
    }

    public static SendFuture removeSessionResult(String sessionKey, Long seqId) {
        ConcurrentHashMap<Long, SendFuture> resultMap = sessionResultMap.get(sessionKey);
        Assert.notNull(resultMap);
        return resultMap.remove(seqId);
    }

    public static SendFuture getSessionResult(String sessionKey, Long seqId) {
        ConcurrentHashMap<Long, SendFuture> resultMap = sessionResultMap.get(sessionKey);
        Assert.notNull(resultMap);
        return resultMap.get(seqId);
    }
}
