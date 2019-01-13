/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.sinope.handler;

import ca.tulip.sinope.core.SinopeDataReadRequest;
import ca.tulip.sinope.core.SinopeDataWriteRequest;
import ca.tulip.sinope.core.appdata.*;
import ca.tulip.sinope.core.internal.SinopeDataAnswer;
import ca.tulip.sinope.util.ByteUtil;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.sinope.SinopeBindingConstants;
import org.openhab.binding.sinope.internal.config.SinopeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * The {@link SinopeDimmerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christos Karras- Initial contribution
 */
public class SinopeDimmerHandler extends BaseThingHandler {

    private static final int DATA_ANSWER = 0x0A;

    private Logger logger = LoggerFactory.getLogger(SinopeDimmerHandler.class);

    private SinopeGatewayHandler gatewayHandler;

    private byte[] deviceId;

    public SinopeDimmerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        try {
            if (SinopeBindingConstants.CHANNEL_DIMMER_OUTPUTINTENSITY.equals(channelUID.getId()) && command instanceof QuantityType) {
                setDimmerOutputIntensity(((QuantityType<?>) command).intValue());
            }
        } catch (IOException e) {
            logger.debug("Cannot handle command for channel {} because of {}", channelUID.getId(),
                    e.getLocalizedMessage());
            this.gatewayHandler.setCommunicationError(true);
        }
    }

    public void setDimmerOutputIntensity(int outputIntensity) throws UnknownHostException, IOException {
        this.gatewayHandler.stopPoll(); // We are about to send something to gateway.
        try {
            if (this.gatewayHandler.connectToBridge()) {
                logger.debug("Connected to bridge");

                SinopeDataWriteRequest req = new SinopeDataWriteRequest(this.gatewayHandler.newSeq(), deviceId,
                        new SinopeOutputIntensityData());
                ((SinopeOutputIntensityData) req.getAppData()).setOutputIntensity(outputIntensity);

                SinopeDataAnswer answ = (SinopeDataAnswer) this.gatewayHandler.execute(req);

                if (answ.getStatus() == DATA_ANSWER) {
                    logger.debug("Output intensity is now: {} %%", outputIntensity);
                } else {
                    logger.debug("Cannot set output intensity, status: {}", answ.getStatus());
                }
            } else {
                logger.debug("Could not connect to bridge to update Output Intensity");
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot connect to bridge");
            }
        } finally {
            this.gatewayHandler.schedulePoll();
        }

    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("bridgeStatusChanged {}", bridgeStatusInfo);
        updateDeviceId();
    }

    @Override
    public void initialize() {
        logger.debug("initializeThing thing {}", getThing().getUID());
        updateDeviceId();
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        updateDeviceId();
    }

    public void updateDimmerOutputIntensity(int outputIntensity) {
        updateState(SinopeBindingConstants.CHANNEL_DIMMER_OUTPUTINTENSITY, new QuantityType<>(outputIntensity, SIUnits.PERCENT));
    }

    public void update() throws UnknownHostException, IOException {
        if (this.deviceId != null) {
            if (isLinked(SinopeBindingConstants.CHANNEL_DIMMER_OUTPUTINTENSITY)) {
                this.updateDimmerOutputIntensity(readOutputIntensity());
            }
        } else {
            logger.error("Device id is null for Thing UID: {}", getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    private int readOutputIntensity() throws UnknownHostException, IOException {
        SinopeDataReadRequest req = new SinopeDataReadRequest(this.gatewayHandler.newSeq(), deviceId,
                new SinopeOutputIntensityData());
        logger.debug("Reading Output Intensity for device id: {}", ByteUtil.toString(deviceId));
        SinopeDataAnswer answ = (SinopeDataAnswer) this.gatewayHandler.execute(req);
        int intensity = ((SinopeOutputIntensityData) answ.getAppData()).getOutputIntensity();
        logger.debug("Output intensity is : {} %", intensity);
        return intensity;
    }

    private void updateDeviceId() {
        String sDeviceId = (String) getConfig().get(SinopeBindingConstants.CONFIG_PROPERTY_DEVICE_ID);
        this.deviceId = SinopeConfig.convert(sDeviceId);
        if (this.deviceId == null) {
            logger.debug("Invalid Device id, cannot convert id: {}", sDeviceId);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid Device id");
            return;
        }
        Bridge bridge = this.getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            return;
        }
        if (this.gatewayHandler == null && ThingStatus.ONLINE.equals(bridge.getStatus())) {
            updateSinopeGatewayHandler(bridge);
        }
        updateStatus(ThingStatus.ONLINE);
    }

    private synchronized void updateSinopeGatewayHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof SinopeGatewayHandler) {
            this.gatewayHandler = (SinopeGatewayHandler) handler;
            this.gatewayHandler.registerDimmerHandler(this);
        }
    }
}
