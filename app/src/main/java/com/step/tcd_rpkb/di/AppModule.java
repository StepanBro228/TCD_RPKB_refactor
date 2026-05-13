package com.step.tcd_rpkb.di;

import android.app.Application;
import android.content.Context;
import android.util.Base64; // Для Basic Auth
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.step.tcd_rpkb.data.datasources.LocalMoveDataSource;
import com.step.tcd_rpkb.data.datasources.RemoteMoveDataSource;
import com.step.tcd_rpkb.data.mapper.InvoiceMapper;
import com.step.tcd_rpkb.data.mapper.MoveItemMapper;
import com.step.tcd_rpkb.data.mapper.MoveResponseMapper;
import com.step.tcd_rpkb.data.mapper.ProductMapper;
import com.step.tcd_rpkb.data.mapper.ChangeMoveStatusMapper;
import com.step.tcd_rpkb.data.network.MoveApiService; // Импорт нашего API сервиса
import com.step.tcd_rpkb.data.repository.MoveRepositoryImpl;
import com.step.tcd_rpkb.data.repository.ServerAvailabilityRepositoryImpl;
import com.step.tcd_rpkb.data.repository.UserSettingsRepositoryImpl;
import com.step.tcd_rpkb.domain.model.Credentials; // Для получения username/password
import com.step.tcd_rpkb.domain.repository.MoveRepository;
import com.step.tcd_rpkb.domain.repository.ServerAvailabilityRepository;
import com.step.tcd_rpkb.domain.repository.UserRepository; // Импорт UserRepository
import com.step.tcd_rpkb.data.repository.UserRepositoryImpl; // Импорт UserRepositoryImpl
import com.step.tcd_rpkb.domain.repository.UserSettingsRepository;
import com.step.tcd_rpkb.domain.util.ConnectivityChecker; // <-- Импорт интерфейса
import com.step.tcd_rpkb.data.util.ConnectivityCheckerImpl; // <-- Импорт реализации
import com.step.tcd_rpkb.data.network.DynamicBaseUrlInterceptor; // <-- Импорт интерсептора
import com.step.tcd_rpkb.data.network.ConnectionRetryInterceptor; // <-- Импорт интерсептора повторных попыток
import com.step.tcd_rpkb.data.network.BasicAuthInterceptor; // <-- Импорт интерсептора авторизации
import com.step.tcd_rpkb.data.network.DeviceNumInterceptor; // <-- Импорт интерсептора номера устройства
import com.step.tcd_rpkb.data.network.FullBodyLoggingInterceptor; // <-- Импорт полного логгера
import com.step.tcd_rpkb.data.datasources.LocalRealmDataSource;
import com.step.tcd_rpkb.utils.SeriesDataManager;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Module
@InstallIn(SingletonComponent.class) // Зависимости будут жить пока живо приложение
public class AppModule {



    @Provides
    @Singleton // Gson обычно создается один раз
    public Gson provideGson() {
        return new GsonBuilder()
                .disableHtmlEscaping() // Отключаем HTML экранирование для корректной работы с кириллицей
                .setPrettyPrinting()
                .serializeNulls() // Сериализуем null значения
                .setLenient() // Позволяем более гибкий парсинг JSON
                .create();
    }

    @Provides
    @Singleton // Retrofit и ApiService
    public Retrofit provideRetrofit(Gson gson, OkHttpClient okHttpClient) {
        //  базовый URL, который будет заменяться DynamicBaseUrlInterceptor
        String defaultBaseUrl = "https://rdc1c-upp/upp82/ru_RU/hs/jsontsd/";
        return new Retrofit.Builder()
                .baseUrl(defaultBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public MoveApiService provideMoveApiService(Retrofit retrofit) {
        return retrofit.create(MoveApiService.class);
    }

    @Provides
    @Singleton
    public ProductMapper provideProductMapper() {
        return new ProductMapper();
    }

    @Provides
    @Singleton
    public MoveItemMapper provideMoveItemMapper() {
        return new MoveItemMapper();
    }

    @Provides
    public InvoiceMapper provideInvoiceMapper(ProductMapper productMapper) {
        return new InvoiceMapper(productMapper);
    }

    @Provides
    public MoveResponseMapper provideMoveResponseMapper(MoveItemMapper moveItemMapper) {
        return new MoveResponseMapper(moveItemMapper);
    }

    @Provides
    public ChangeMoveStatusMapper provideChangeMoveStatusMapper() {
        return new ChangeMoveStatusMapper();
    }


    @Provides
    @Singleton
    public UserSettingsRepository provideUserSettingsRepository(@ApplicationContext Context appContext) {
        return new UserSettingsRepositoryImpl(appContext);
    }

    @Provides
    @Singleton
    public LocalMoveDataSource provideLocalMoveDataSource(@ApplicationContext Context appContext, Gson gson, InvoiceMapper invoiceMapper, MoveResponseMapper moveResponseMapper, ProductMapper productMapper, MoveItemMapper moveItemMapper) {
        return new LocalMoveDataSource(appContext, gson, invoiceMapper, moveResponseMapper);
    }

    @Provides
    @Singleton
    public RemoteMoveDataSource provideRemoteMoveDataSource(MoveApiService moveApiService, MoveResponseMapper moveResponseMapper, InvoiceMapper invoiceMapper, ChangeMoveStatusMapper changeMoveStatusMapper) {
        return new RemoteMoveDataSource(moveApiService, moveResponseMapper, invoiceMapper, changeMoveStatusMapper);
    }

    @Provides
    @Singleton
    public DynamicBaseUrlInterceptor provideDynamicBaseUrlInterceptor(UserSettingsRepository userSettingsRepository) {
        return new DynamicBaseUrlInterceptor(userSettingsRepository);
    }
    
    @Provides
    @Singleton
    public ConnectionRetryInterceptor provideConnectionRetryInterceptor() {
        return new ConnectionRetryInterceptor();
    }
    
    @Provides
    @Singleton
    public BasicAuthInterceptor provideBasicAuthInterceptor(UserSettingsRepository userSettingsRepository) {
        return new BasicAuthInterceptor(userSettingsRepository);
    }

    @Provides
    @Singleton
    public DeviceNumInterceptor provideDeviceNumInterceptor(UserSettingsRepository userSettingsRepository) {
        return new DeviceNumInterceptor(userSettingsRepository);
    }


    // Создаем TrustManager, который игнорирует проверку сертификатов
    private X509TrustManager createInsecureTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                // доверяем всем клиентским сертификатам
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                //  доверяем всем серверным сертификатам
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
    public OkHttpClient provideOkHttpClient(UserSettingsRepository userSettingsRepository, 
                                          DynamicBaseUrlInterceptor dynamicBaseUrlInterceptor,
                                          ConnectionRetryInterceptor retryInterceptor,
                                          BasicAuthInterceptor basicAuthInterceptor,
                                          DeviceNumInterceptor deviceNumInterceptor) {
        // Используем наш кастомный интерцептор для полного логирования
        FullBodyLoggingInterceptor fullLoggingInterceptor = new FullBodyLoggingInterceptor();

        // Создаем доверяющий всем сертификатам TrustManager
        X509TrustManager trustManager = createInsecureTrustManager();
        SSLSocketFactory sslSocketFactory = createInsecureSSLSocketFactory(trustManager);

        android.util.Log.d("AppModule", "Создаем OkHttpClient с увеличенным таймаутом и игнорированием SSL-сертификатов");

        // Увеличиваем таймауты для запросов
        return new OkHttpClient.Builder()

                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)

                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)

                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                // Отключаем проверку SSL-сертификатов
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier((hostname, session) -> true) // Игнорируем проверку имени хоста
                .addInterceptor(dynamicBaseUrlInterceptor) // Добавляем интерсептор для динамического изменения URL
                .addInterceptor(basicAuthInterceptor) // Добавляем интерсептор для Basic авторизации
                .addInterceptor(deviceNumInterceptor) // Добавляем интерсептор для номера устройства
                .addInterceptor(fullLoggingInterceptor) // Добавляем полный логгер без ограничений
                .addInterceptor(retryInterceptor) // Добавляем интерсептор для повторных попыток
                .build();
    }

    @Provides
    @Singleton
    public ServerAvailabilityRepository provideServerAvailabilityRepository(
            MoveApiService moveApiService,
            ConnectivityChecker connectivityChecker
    ) {
        return new ServerAvailabilityRepositoryImpl(moveApiService, connectivityChecker);
    }

    @Provides
    @Singleton
    public ConnectivityChecker provideConnectivityChecker(
            @ApplicationContext Context appContext
    ) {
        return new ConnectivityCheckerImpl(appContext);
    }

    @Provides
    @Singleton
    public SeriesDataManager provideSeriesDataManager(
            @ApplicationContext Context appContext,
            LocalRealmDataSource localRealmDataSource
    ) {
        return new SeriesDataManager(appContext, localRealmDataSource);
    }
} 