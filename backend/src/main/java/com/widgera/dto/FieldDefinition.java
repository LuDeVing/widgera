package com.widgera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Field name is required")
    private String name;

    @NotBlank(message = "Field type is required")
    @Pattern(regexp = "^(string|number)$", message = "Field type must be 'string' or 'number'")
    private String type;
}
