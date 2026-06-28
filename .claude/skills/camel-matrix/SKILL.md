---

name: camel-matrix
description: Generates an AsciiDoc compatibility matrix for Apache Camel Spring Boot, Spring Boot, and Apache CXF versions by running the camel-springboot-matrix.sh script. Use when the user asks to generate or update the Camel compatibility matrix, check Camel Spring Boot version compatibility, or run the camel-springboot-matrix script. Supports optional version range arguments (min max).
--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Camel Spring Boot Compatibility Matrix

Run `.run/scripts/camel-springboot-matrix.sh` to generate `camel-springboot-matrix.adoc`.

## Arguments

The user may provide an optional version range: `$ARGUMENTS`

- No arguments: all available versions are processed
- Two arguments (e.g. `4.0.0 4.15.0`): only versions in that range are processed

## Steps

1. Run the script:

```bash
bash .run/scripts/camel-springboot-matrix.sh $ARGUMENTS
```

2. After completion, report:
   - How many Camel versions were processed
   - That `camel-springboot-matrix.adoc` was created/updated in the current directory
   - The last 5 rows of the generated table so the user can verify the output

