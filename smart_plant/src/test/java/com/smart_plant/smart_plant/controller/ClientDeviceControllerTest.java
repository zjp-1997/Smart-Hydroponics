package com.smart_plant.smart_plant.controller;

import com.smart_plant.smart_plant.dto.ClientDeviceControlStatusRequest;
import com.smart_plant.smart_plant.response.R;
import com.smart_plant.smart_plant.service.IotDeviceService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClientDeviceControllerTest {

    @Test
    void forwardsPlotDeviceControlStatusToService() {
        IotDeviceService service = mock(IotDeviceService.class);
        ClientDeviceController controller = new ClientDeviceController(service);
        ClientDeviceControlStatusRequest request = new ClientDeviceControlStatusRequest();
        request.setControlStatus(1);

        R<Void> response = controller.updateControlStatus(9L, request);

        assertEquals(200, response.getCode());
        verify(service).updateControlStatus(9L, 1);
    }
}
