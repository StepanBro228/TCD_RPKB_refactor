package com.step.tcd_rpkb.data.di;

import com.google.gson.Gson;
import com.step.tcd_rpkb.data.network.AuthApiService;
import com.step.tcd_rpkb.data.network.AuthorizationInterceptor;
import com.step.tcd_rpkb.data.network.ConnectionRetryInterceptor;
import com.step.tcd_rpkb.data.network.DeviceNumInterceptor;
import com.step.tcd_rpkb.data.network.DynamicBaseUrlInterceptor;
import com.step.tcd_rpkb.data.network.UserAuthApiService;
import com.step.tcd_rpkb.data.network.UserAuthInterceptor;
import com.step.tcd_rpkb.data.network.FullBodyLoggingInterceptor;
import com.step.tcd_rpkb.data.repository.AuthRepositoryImpl;
import com.step.tcd_rpkb.domain.repository.AuthRepository;
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Модуль Dagger Hilt для инъекции зависимостей авторизации
 */
@Module
@InstallIn(SingletonComponent.class)
public class AuthModule {
    
    @Provides
    @Singleton
    public AuthorizationInterceptor provideAuthorizationInterceptor() {
        return new AuthorizationInterceptor();
    }
    
    @Provides
    @Singleton
    public UserAuthInterceptor provideUserAuthInterceptor() {
        return new UserAuthInterceptor();
    }
    

    
    // Создаем TrustManager, который игнорирует проверку сертификатов
    private X509TrustManager createInsecureTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // Ничего не делаем, доверяем всем клиентским сертификатам
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // Ничего не делаем, доверяем всем серверным сертификатам
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }

    // Создаем небезопасный SSLSocketFactory
    private SSLSocketFactory createInsecureSSLSocketFactory(X509TrustManager trustManager) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create insecure SSL socket factory", e);
        }
    }
    
    @Provides
    @Singleton
    @Named("auth")
    public OkHttpClient provideAuthOkHttpClient(UserSettingsRepository userSettingsRepository,
                                               DynamicBaseUrlInterceptor dynamicBaseUrlInterceptor,
                                               ConnectionRetryInterceptor retryInterceptor,
                                               AuthorizationInterceptor authorizationInterceptor,
                                               DeviceNumInterceptor deviceNumInterceptor) {
        FullBodyLoggingInterceptor fullLoggingInterceptor = new FullBodyLoggingInterceptor();

        // Создаем доверяющий всем сертификатам TrustManager
        X509TrustManager trustManager = createInsecureTrustManager();
        SSLSocketFactory sslSocketFactory = createInsecureSSLSocketFactory(trustManager);

        return new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier((hostname, session) -> true)
                .addInterceptor(dynamicBaseUrlInterceptor)
                .addInterceptor(authorizationInterceptor) // Используем фиксированную авторизацию
                .addInterceptor(deviceNumInterceptor) // Добавляем номер устройства
                .addInterceptor(fullLoggingInterceptor)
                .addInterceptor(retryInterceptor)
                .build();
    }
    
    @Provides
    @Singleton
    @Named("auth")
    public Retrofit provideAuthRetrofit(@Named("auth") OkHttpClient okHttpClient, 
                                       Gson gson,
                                       UserSettingsRepository userSettingsRepository) {
        String defaultBaseUrl = userSettingsRepository.getDatabaseURL();
        return new Retrofit.Builder()
                .baseUrl(defaultBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
    
    @Provides
    @Singleton
    @Named("userAuth")
    public OkHttpClient provideUserAuthOkHttpClient(UserSettingsRepository userSettingsRepository,
                                                   DynamicBaseUrlInterceptor dynamicBaseUrlInterceptor,
                                                   ConnectionRetryInterceptor retryInterceptor,
                                                   UserAuthInterceptor userAuthInterceptor,
                                                   DeviceNumInterceptor deviceNumInterceptor) {
        FullBodyLoggingInterceptor fullLoggingInterceptor = new FullBodyLoggingInterceptor();

        // Создаем доверяющий всем сертификатам TrustManager
        X509TrustManager trustManager = createInsecureTrustManager();
        SSLSocketFactory sslSocketFactory = createInsecureSSLSocketFactory(trustManager);

        return new OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier((hostname, session) -> true)
                .addInterceptor(dynamicBaseUrlInterceptor)
                .addInterceptor(userAuthInterceptor) // Используем пользовательскую авторизацию
                .addInterceptor(deviceNumInterceptor) // Добавляем номер устройства
                .addInterceptor(fullLoggingInterceptor)
                .addInterceptor(retryInterceptor)
                .build();
    }
    
    @Provides
    @Singleton
    @Named("userAuth")
    public Retrofit provideUserAuthRetrofit(@Named("userAuth") OkHttpClient okHttpClient, 
                                           Gson gson,
                                           UserSettingsRepository userSettingsRepository) {
        String defaultBaseUrl = userSettingsRepository.getDatabaseURL();
        return new Retrofit.Builder()
                .baseUrl(defaultBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
    
    @Provides
    @Singleton
    public AuthApiService provideAuthApiService(@Named("auth") Retrofit retrofit) {
        return retrofit.create(AuthApiService.class);
    }
    
    @Provides
    @Singleton
    public UserAuthApiService provideUserAuthApiService(@Named("userAuth") Retrofit retrofit) {
        return retrofit.create(UserAuthApiService.class);
    }
    
    @Provides
    @Singleton
    public AuthRepository provideAuthRepository(AuthRepositoryImpl authRepositoryImpl) {
        return authRepositoryImpl;
    }
} 