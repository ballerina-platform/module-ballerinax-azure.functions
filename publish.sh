./gradlew clean build -x check -x test
rm -rf ~/.ballerina/repositories/local/
cd ballerina
#bal test
/home/anjana/repos/module-ballerinax-azure.functions/target/ballerina-runtime/bin/bal pack
/home/anjana/repos/module-ballerinax-azure.functions/target/ballerina-runtime/bin/bal push --repository local
