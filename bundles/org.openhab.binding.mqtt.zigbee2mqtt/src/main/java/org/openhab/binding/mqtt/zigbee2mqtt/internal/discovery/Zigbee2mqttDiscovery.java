/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mqtt.zigbee2mqtt.internal.discovery;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.binding.mqtt.discovery.AbstractMQTTDiscovery;
import org.openhab.binding.mqtt.discovery.MQTTTopicDiscoveryService;
import org.openhab.binding.mqtt.zigbee2mqtt.internal.Zigbee2mqttBindingConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Zigbee2mqttDiscovery} is responsible for discovering Zigbee2mqtt coordinators.
 *
 * @author Aitor Iturrioz - Initial contribution
 */
@Component(immediate = true, service = DiscoveryService.class, configurationPid = "discovery.zigbee2mqtt")
@NonNullByDefault
public class Zigbee2mqttDiscovery extends AbstractMQTTDiscovery {
    private final Logger logger = LoggerFactory.getLogger(Zigbee2mqttDiscovery.class);

    protected final MQTTTopicDiscoveryService discoveryService;

    @Activate
    public Zigbee2mqttDiscovery(@Reference MQTTTopicDiscoveryService discoveryService) {
        super(Collections.singleton(Zigbee2mqttBindingConstants.COORDINATOR_THING), 3, true,
                "zigbee2mqtt/bridge/state");
        this.discoveryService = discoveryService;
    }

    @Override
    protected MQTTTopicDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    /**
     * @param topic A topic like "zigbee2mqtt/mydevice/state"
     * @return Returns the "mydevice" part of the example
     */
    public static @Nullable String extractCoordinatorID(String topic) {
        String[] strings = topic.split("/");
        if (strings.length > 2) {
            return strings[1];
        }
        return null;
    }

    @Override
    public void receivedMessage(ThingUID connectionBridge, MqttBrokerConnection connection, String topic,
            byte[] payload) {
        resetTimeout();

        final String coordinatorID = extractCoordinatorID(topic);
        if (coordinatorID == null) {
            logger.trace("Found zigbee2mqtt device. But coordinatorID {} is invalid.", coordinatorID);
            return;
        }

        publishDevice(connectionBridge, connection, coordinatorID, topic, coordinatorID);

    }

    void publishDevice(ThingUID connectionBridge, MqttBrokerConnection connection, String coordinatorID, String topic,
            String name) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("coordinatorId", coordinatorID);
        properties.put("baseTopic", topic.substring(0, topic.indexOf("/")));

        thingDiscovered(DiscoveryResultBuilder
                .create(new ThingUID(Zigbee2mqttBindingConstants.COORDINATOR_THING, connectionBridge, coordinatorID))
                .withBridge(connectionBridge).withProperties(properties).withRepresentationProperty("coordinatorId")
                .withLabel(name).build());
    }

    @Override
    public void topicVanished(ThingUID connectionBridge, MqttBrokerConnection connection, String topic) {
        String coordinatorID = extractCoordinatorID(topic);
        if (coordinatorID == null) {
            return;
        }
        thingRemoved(new ThingUID(Zigbee2mqttBindingConstants.COORDINATOR_THING, connectionBridge, coordinatorID));
    }
}
