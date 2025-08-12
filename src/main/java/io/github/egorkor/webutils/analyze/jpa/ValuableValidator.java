package io.github.egorkor.webutils.analyze.jpa;

import lombok.Getter;

import java.util.Map;

@Getter
public class ValuableValidator extends Validator {
    private Map<String, ?> constraints;

    public ValuableValidator(ValidatorCode validatorCode, Map<String, ?> constraints) {
        super(validatorCode);
        this.constraints = constraints;
    }

    public static ValuableValidatorBuilder builder() {
        return new ValuableValidatorBuilder();
    }

    public static class ValuableValidatorBuilder extends ValidatorBuilder {
        private Map<String, ?> constraints;
        private ValidatorCode code;

        ValuableValidatorBuilder() {
        }

        @Override
        public ValuableValidatorBuilder validatorCode(ValidatorCode validatorCode) {
            this.code = validatorCode;
            return this;
        }

        public ValuableValidatorBuilder constraints(Map<String, ?> constraints) {
            this.constraints = constraints;
            return this;
        }

        public ValuableValidator build() {
            return new ValuableValidator(this.code, this.constraints);
        }
    }

    @Override
    public String toString() {
        return "ValuableValidator (code=" + this.getValidatorCode() + ",constraints=" + constraints + ")";
    }
}
