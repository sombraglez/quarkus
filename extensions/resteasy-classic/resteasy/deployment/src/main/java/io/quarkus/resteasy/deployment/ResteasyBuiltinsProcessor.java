package io.quarkus.resteasy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.runtime.CompositeExceptionMapper;
import io.quarkus.resteasy.runtime.EagerSecurityFilter;
import io.quarkus.resteasy.runtime.ExceptionMapperRecorder;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsPermissionChecker;
import io.quarkus.resteasy.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.runtime.NotFoundExceptionMapper;
import io.quarkus.resteasy.runtime.SecurityContextFilter;
import io.quarkus.resteasy.runtime.StandardSecurityCheckInterceptor;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.runtime.vertx.JsonArrayReader;
import io.quarkus.resteasy.runtime.vertx.JsonArrayWriter;
import io.quarkus.resteasy.runtime.vertx.JsonObjectReader;
import io.quarkus.resteasy.runtime.vertx.JsonObjectWriter;
import io.quarkus.security.spi.DefaultSecurityCheckBuildItem;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.devmode.NotFoundPageDisplayableEndpointBuildItem;
import io.quarkus.vertx.http.deployment.devmode.RouteDescriptionBuildItem;
import io.quarkus.vertx.http.runtime.devmode.AdditionalRouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;

public class ResteasyBuiltinsProcessor {

    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @BuildStep
    void setUpDenyAllJaxRs(JaxRsSecurityConfig securityConfig,
            BuildProducer<DefaultSecurityCheckBuildItem> defaultSecurityCheckProducer) {
        if (securityConfig.denyJaxRs) {
            defaultSecurityCheckProducer.produce(DefaultSecurityCheckBuildItem.denyAll());
        } else if (securityConfig.defaultRolesAllowed.isPresent()) {
            defaultSecurityCheckProducer
                    .produce(DefaultSecurityCheckBuildItem.rolesAllowed(securityConfig.defaultRolesAllowed.get()));
        }
    }

    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setUpSecurity(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItem, Capabilities capabilities) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(UnauthorizedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(ForbiddenExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationFailedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationRedirectExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationCompletionExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(CompositeExceptionMapper.class.getName()));
        if (capabilities.isPresent(Capability.SECURITY)) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(SecurityContextFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(SecurityContextFilter.class));
            providers.produce(new ResteasyJaxrsProviderBuildItem(EagerSecurityFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(EagerSecurityFilter.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(JaxRsPermissionChecker.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.RolesAllowedInterceptor.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem
                    .unremovableOf(StandardSecurityCheckInterceptor.PermissionsAllowedInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.PermitAllInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.AuthenticatedInterceptor.class));
        }
    }

    @BuildStep
    void vertxProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        // These providers should work even if jackson-databind is not on the classpath
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayWriter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectWriter.class.getName()));
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void setupExceptionMapper(BuildProducer<ResteasyJaxrsProviderBuildItem> providers, HttpRootPathBuildItem httpRoot,
            ExceptionMapperRecorder recorder) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(NotFoundExceptionMapper.class.getName()));
        recorder.setHttpRoot(httpRoot.getRootPath());
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addStaticResourcesExceptionMapper(ApplicationArchivesBuildItem applicationArchivesBuildItem,
            ExceptionMapperRecorder recorder) {
        recorder.setStaticResourceRoots(applicationArchivesBuildItem.getAllApplicationArchives().stream()
                .map(i -> i.apply(t -> {
                    var p = t.getPath(META_INF_RESOURCES);
                    return p == null ? null : p.toAbsolutePath().toString();
                }))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addAdditionalEndpointsExceptionMapper(List<NotFoundPageDisplayableEndpointBuildItem> displayableEndpoints,
            ExceptionMapperRecorder recorder, HttpRootPathBuildItem httpRoot) {
        List<AdditionalRouteDescription> endpoints = displayableEndpoints
                .stream()
                .map(displayableAdditionalBuildItem -> new AdditionalRouteDescription(
                        displayableAdditionalBuildItem.getEndpoint(httpRoot), displayableAdditionalBuildItem.getDescription()))
                .sorted()
                .collect(Collectors.toList());

        recorder.setAdditionalEndpoints(endpoints);
    }

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void addReactiveRoutesExceptionMapper(List<RouteDescriptionBuildItem> routeDescriptions,
            ExceptionMapperRecorder recorder, HttpRootPathBuildItem httpRoot) {
        List<RouteDescription> reactiveRoutes = new ArrayList<>();
        for (RouteDescriptionBuildItem description : routeDescriptions) {
            reactiveRoutes.add(description.getDescription());
        }
        recorder.setReactiveRoutes(reactiveRoutes);
    }
}
