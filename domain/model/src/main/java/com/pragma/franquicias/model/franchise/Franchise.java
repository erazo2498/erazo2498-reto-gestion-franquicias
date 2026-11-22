package com.pragma.franquicias.model.franchise;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Franchise {
    UUID id;
    String name;
}
