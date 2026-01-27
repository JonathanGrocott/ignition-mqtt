# Vendor Name Display in Ignition Modules

## Important Finding

After investigation, we discovered that **vendor names in Ignition's module list are only displayed for modules signed with certificates from recognized Certificate Authorities (CAs)**.

## Why Self-Signed Certificates Don't Work

- Ignition rejects self-signed certificates with the error: "No certificate found"
- Even if you manually trust a self-signed certificate, Ignition may not display the vendor name
- The vendor name field appears to be reserved for commercially signed modules

## Current Situation

This module is built **unsigned** for development use. The vendor name field will remain empty in the Ignition Gateway module list.

## Options for Displaying Vendor Name

### Option 1: Obtain a Code Signing Certificate (Production)

To display "J.Grocott" as the vendor name in production:

1. **Purchase a code signing certificate** from a recognized CA such as:
   - DigiCert
   - Sectigo
   - GlobalSign
   
2. **Request the certificate with your information:**
   - Organization: J.Grocott
   - Common Name: J.Grocott (or your full name)

3. **Configure module signing** in `gradle.properties`:
   ```properties
   ignition.signing.keystoreFile=path/to/your-certificate.p12
   ignition.signing.keystorePassword=your-password
   ignition.signing.certAlias=your-alias
   ignition.signing.certPassword=your-cert-password
   ignition.signing.certFile=path/to/certificate-chain.pem
   ```

4. **Enable signing** in `build.gradle.kts`:
   ```kotlin
   skipModlSigning.set(false)
   ```

### Option 2: Use Unsigned Modules (Development)

For development and testing:
- Keep `skipModlSigning.set(true)` in `build.gradle.kts`
- Accept that the vendor name will not be displayed
- Focus on functionality rather than presentation

## What We Tried

1. ✅ Created self-signed certificate with O=J.Grocott
2. ✅ Configured module signing in Gradle
3. ✅ Successfully signed the module locally
4. ❌ Ignition rejected the self-signed certificate: "No certificate found"

## Conclusion

**For this open-source/development module**, we recommend using unsigned modules. The vendor name is a cosmetic feature that requires a commercial certificate investment which may not be worthwhile for a free/open-source project.

**For commercial deployment**, invest in a proper code signing certificate to display your vendor name professionally.

## Current Module Configuration

- Version: 1.0.2
- Signing: Disabled (unsigned modules)
- Vendor Name: Not displayed (requires commercial certificate)
- Module XML: Contains `<vendor>J.Grocott</vendor>` tag (but not displayed by Ignition for unsigned modules)
