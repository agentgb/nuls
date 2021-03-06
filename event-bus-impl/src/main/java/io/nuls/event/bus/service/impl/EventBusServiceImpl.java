/**
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.event.bus.service.impl;

import io.nuls.core.exception.NulsException;
import io.nuls.core.utils.log.Log;
import io.nuls.event.bus.constant.EventCategoryEnum;
import io.nuls.event.bus.handler.AbstractEventHandler;
import io.nuls.event.bus.handler.intf.NulsEventHandler;
import io.nuls.event.bus.service.entity.EventItem;
import io.nuls.event.bus.service.intf.EventBusService;
import io.nuls.protocol.event.base.BaseEvent;
import io.nuls.protocol.event.manager.EventManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Niels
 * @date 2018/1/5
 */
public class EventBusServiceImpl implements EventBusService {

    private LocalEventService localService = LocalEventService.getInstance();
    private NetworkEventService networkService = NetworkEventService.getInstance();

    @Override
    public String subscribeEvent(Class<? extends BaseEvent> eventClass, NulsEventHandler<? extends BaseEvent> eventHandler) {
        String id = localService.registerEventHandler(eventClass,
                (AbstractEventHandler<? extends BaseEvent>) eventHandler);
        networkService.registerEventHandler(id, eventClass,
                (AbstractEventHandler<? extends BaseEvent>) eventHandler);
        return id;
    }


    @Override
    public void unsubscribeEvent(String subcribeId) {
        this.localService.removeEventHandler(subcribeId);
        this.networkService.removeEventHandler(subcribeId);
    }

    @Override
    public void publishEvent(EventCategoryEnum category, byte[] bytes, String fromId) throws IllegalAccessException, NulsException, InstantiationException {
        if (category == EventCategoryEnum.LOCAL) {
            BaseEvent event = EventManager.getInstance(bytes);
            this.publishLocalEvent(event);
        } else {
            this.publishNetworkEvent(bytes, fromId);
        }
    }

    @Override
    public void publishEvent(EventCategoryEnum category, BaseEvent event, String fromId) {
        if (category == EventCategoryEnum.LOCAL) {
            this.publishLocalEvent((BaseEvent) event);
        } else {
            this.publishNetworkEvent((BaseEvent) event, fromId);
        }
    }

    @Override
    public void publishNetworkEvent(byte[] bytes, String fromId) {
        try {
            networkService.publish(bytes, fromId);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public void publishNetworkEvent(BaseEvent event, String fromId) {
        try {
            networkService.publish(event, fromId);
        } catch (Exception e) {
            Log.error(e);
        }
    }

    @Override
    public void publishLocalEvent(BaseEvent event) {
        localService.publish(event);
    }

    @Override
    public List<EventItem> getEventList() {
        List<EventItem> list = new ArrayList<>();
        Set<String> keySet = new HashSet<>(EventManager.getEventMap().keySet());
        for(String key:keySet){
            EventItem item = new EventItem();
            item.setClazz(EventManager.getEventMap().get(key));
            item.setName(item.getClazz().getSimpleName());
            String[] array = key.split(EventManager.KEY_SPLIT);
            short moduleId = Short.parseShort(array[0]);
            short eventType = Short.parseShort(array[1]);
            item.setEventType(moduleId);
            item.setModuleId(eventType);
            list.add(item);
        }
        return list;
    }

}
