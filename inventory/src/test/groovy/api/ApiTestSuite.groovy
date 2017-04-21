package api

import org.folio.inventory.InventoryVerticle
import org.folio.metadata.common.VertxAssistant
import org.folio.metadata.common.testing.HttpClient
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite
import support.fakes.FakeOkapi

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@RunWith(Suite.class)

@Suite.SuiteClasses([
  InstancesApiExamples.class,
  ItemApiExamples.class,
  ModsIngestExamples.class
])

public class ApiTestSuite {
  public static final INVENTORY_VERTICLE_TEST_PORT = 9603
  public static final String TENANT_ID = "test_tenant"

  private static final String TOKEN = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInRlbmFudCI6ImRlbW9fdGVuYW50In0.29VPjLI6fLJzxQW0UhQ0jsvAn8xHz501zyXAxRflXfJ9wuDzT8TDf-V75PjzD7fe2kHjSV2dzRXbstt3BTtXIQ"

  private static VertxAssistant vertxAssistant = new VertxAssistant();
  private static String inventoryModuleDeploymentId
  private static String fakeModulesDeploymentId

  private static Boolean useOkapiForApiRequests =
    (System.getProperty("use.okapi.initial.requests") ?: "").toBoolean()
  private static Boolean useOkapiForStorageRequests =
    (System.getProperty("use.okapi.storage.requests") ?: "").toBoolean()
  private static String okapiAddress = System.getProperty("okapi.address", "")

  @BeforeClass
  public static void before() {

    println("Use Okapi For Initial Requests:" + System.getProperty("use.okapi.initial.requests"))
    println("Use Okapi For Storage Requests:" + System.getProperty("use.okapi.storage.requests"))

    startVertx()
    startFakeModules()
    startInventoryVerticle()
  }

  @AfterClass
  public static void after() {
    stopInventoryVerticle()
    stopFakeModules()
    stopVertx()
  }

  static HttpClient createHttpClient() {
    new HttpClient(storageOkapiUrl(), TENANT_ID, TOKEN)
  }

  static String storageOkapiUrl() {
    if(useOkapiForStorageRequests) {
      okapiAddress
    }
    else {
      FakeOkapi.address
    }
  }

  static String apiRoot() {
    def directRoot = "http://localhost:${ApiTestSuite.INVENTORY_VERTICLE_TEST_PORT}"

    useOkapiForApiRequests ? okapiAddress : directRoot
  }

  private static stopVertx() {
    vertxAssistant.stop()
  }

  private static startVertx() {
    vertxAssistant.start()
  }

  private static startInventoryVerticle() {
    def deployed = new CompletableFuture()

    def storageType = "okapi"
    def storageLocation = ""

    println("Storage Type: ${storageType}")
    println("Storage Location: ${storageLocation}")

    def config = ["port": INVENTORY_VERTICLE_TEST_PORT,
                  "storage.type" : storageType,
                  "storage.location" : storageLocation]

    vertxAssistant.deployGroovyVerticle(
      InventoryVerticle.class.name, config,  deployed)

    inventoryModuleDeploymentId = deployed.get(20000, TimeUnit.MILLISECONDS)
  }

  private static stopInventoryVerticle() {
    def undeployed = new CompletableFuture()

    vertxAssistant.undeployVerticle(inventoryModuleDeploymentId, undeployed)

    undeployed.get(20000, TimeUnit.MILLISECONDS)
  }

  private static startFakeModules() {
    if(!useOkapiForStorageRequests) {
      def fakeModulesDeployed = new CompletableFuture<>();

        vertxAssistant.deployVerticle(FakeOkapi.class.getName(),
          [:], fakeModulesDeployed);

      fakeModulesDeploymentId = fakeModulesDeployed.get(10, TimeUnit.SECONDS);
    }
  }

  private static stopFakeModules() {
    if(!useOkapiForStorageRequests) {
      def undeployed = new CompletableFuture()

      vertxAssistant.undeployVerticle(fakeModulesDeploymentId, undeployed)

      undeployed.get(20000, TimeUnit.MILLISECONDS)
    }
  }
}
