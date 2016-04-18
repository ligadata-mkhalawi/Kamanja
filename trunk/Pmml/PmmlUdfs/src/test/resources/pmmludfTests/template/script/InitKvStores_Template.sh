java -jar {InstallDirectory}/bin/KVInit-1.0 --kvname System.SputumCodes        --config {InstallDirectory}/config/Engine1Config.properties --csvpath {InstallDirectory}/input/application1/data/sputumCodes.csv       --keyfieldname icd9Code
java -jar {InstallDirectory}/bin/KVInit-1.0 --kvname System.SmokeCodes         --config {InstallDirectory}/config/Engine1Config.properties --csvpath {InstallDirectory}/input/application1/data/smokingCodes.csv      --keyfieldname icd9Code
java -jar {InstallDirectory}/bin/KVInit-1.0 --kvname System.EnvCodes           --config {InstallDirectory}/config/Engine1Config.properties --csvpath {InstallDirectory}/input/application1/data/envExposureCodes.csv  --keyfieldname icd9Code
java -jar {InstallDirectory}/bin/KVInit-1.0 --kvname System.CoughCodes         --config {InstallDirectory}/config/Engine1Config.properties --csvpath {InstallDirectory}/input/application1/data/coughCodes.csv        --keyfieldname icd9Code
java -jar {InstallDirectory}/bin/KVInit-1.0 --kvname System.DyspnoeaCodes      --config {InstallDirectory}/config/Engine1Config.properties --csvpath {InstallDirectory}/input/application1/data/dyspnoea.csv          --keyfieldname icd9Code
