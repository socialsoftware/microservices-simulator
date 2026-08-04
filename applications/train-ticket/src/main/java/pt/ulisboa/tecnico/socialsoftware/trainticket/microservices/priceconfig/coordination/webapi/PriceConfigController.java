package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.functionalities.PriceConfigFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi.requestDtos.CreatePriceConfigRequestDto;

@RestController
public class PriceConfigController {
    @Autowired
    private PriceConfigFunctionalities priceConfigFunctionalities;

    @PostMapping("/priceconfigs/create")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceConfigDto createPriceConfig(@RequestBody CreatePriceConfigRequestDto createRequest) {
        return priceConfigFunctionalities.createPriceConfig(createRequest);
    }

    @GetMapping("/priceconfigs/{priceconfigAggregateId}")
    public PriceConfigDto getPriceConfigById(@PathVariable Integer priceconfigAggregateId) {
        return priceConfigFunctionalities.getPriceConfigById(priceconfigAggregateId);
    }

    @PutMapping("/priceconfigs")
    public PriceConfigDto updatePriceConfig(@RequestBody PriceConfigDto priceconfigDto) {
        return priceConfigFunctionalities.updatePriceConfig(priceconfigDto);
    }

    @DeleteMapping("/priceconfigs/{priceconfigAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePriceConfig(@PathVariable Integer priceconfigAggregateId) {
        priceConfigFunctionalities.deletePriceConfig(priceconfigAggregateId);
    }

    @GetMapping("/priceconfigs")
    public List<PriceConfigDto> getAllPriceConfigs() {
        return priceConfigFunctionalities.getAllPriceConfigs();
    }
}
