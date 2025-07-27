package io.github.egorkor.webutils.analyze.jpa;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ValuableValidator extends Validator {
    private Map<String, ?> constraints;

    public static ValuableValidatorBuilder builder() {
        return new ValuableValidatorBuilder();
    }

    public static class ValuableValidatorBuilder extends ValidatorBuilder {
        private Map<String, ?> constraints;

        ValuableValidatorBuilder() {
        }

        @Override
        public ValuableValidatorBuilder validatorCode(ValidatorCode validatorCode) {
            super.validatorCode(validatorCode);
            return this;
        }

        public ValuableValidatorBuilder constraints(Map<String, ?> constraints) {
            this.constraints = constraints;
            return this;
        }

        public ValuableValidator build() {
            return new ValuableValidator(this.constraints);
        }

        public String toString() {
            return "ValuableValidator.ValuableValidatorBuilder(constraints=" + this.constraints + ")";
        }
    }
}
