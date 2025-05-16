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
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor; // Для логгирования
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class) // Зависимости будут жить пока живо приложение
public class AppModule {

    // TODO: Вынести базовый URL в BuildConfig или другой механизм конфигурации
    private static final String BASE_URL = "http://rdc1c-upp.rpkb.ru/upp82/hs/json/";

    @Provides
    @Singleton // Gson обычно создается один раз
    public Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton // Retrofit и ApiService тоже обычно Singleton
    public Retrofit provideRetrofit(Gson gson, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
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
    @Singleton // Добавим Singleton, если ProductMapper тоже Singleton
    public ProductMapper provideProductMapper() {
        return new ProductMapper();
    }

    @Provides
    @Singleton // Добавим Singleton, если MoveItemMapper тоже Singleton
    public MoveItemMapper provideMoveItemMapper() {
        return new MoveItemMapper();
    }

    @Provides // Мапперы обычно не требуют состояния, Singleton не обязателен, но и не вреден
    public InvoiceMapper provideInvoiceMapper(ProductMapper productMapper) {
        return new InvoiceMapper(productMapper);
    }

    @Provides
    public MoveResponseMapper provideMoveResponseMapper(MoveItemMapper moveItemMapper) {
        return new MoveResponseMapper(moveItemMapper);
    }

    // Предоставление UserSettingsRepository
    // UserSettingsRepositoryImpl принимает Context
    @Provides
    @Singleton
    public UserSettingsRepository provideUserSettingsRepository(@ApplicationContext Context appContext) {
        return new UserSettingsRepositoryImpl(appContext);
    }

    @Provides
    @Singleton // DataSources могут быть Singleton, если не имеют изменяемого состояния, специфичного для сессии
    public LocalMoveDataSource provideLocalMoveDataSource(@ApplicationContext Context appContext, Gson gson, InvoiceMapper invoiceMapper, MoveResponseMapper moveResponseMapper, ProductMapper productMapper, MoveItemMapper moveItemMapper) {
        return new LocalMoveDataSource(appContext, gson, invoiceMapper, moveResponseMapper);
    }

    @Provides
    @Singleton
    public RemoteMoveDataSource provideRemoteMoveDataSource(MoveApiService moveApiService, MoveResponseMapper moveResponseMapper, InvoiceMapper invoiceMapper, MoveItemMapper moveItemMapper, ProductMapper productMapper) {
        return new RemoteMoveDataSource(moveApiService, moveResponseMapper, invoiceMapper);
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(UserSettingsRepository userSettingsRepository) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Уровень логгирования

        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor) // Добавляем логгер
                .addInterceptor(chain -> { // Interceptor для Basic Auth
                    Credentials credentials = userSettingsRepository.getCredentials(); // Получаем актуальные креды
                    String username = credentials.getUsername();
                    String password = credentials.getPassword();
                    Request originalRequest = chain.request();
                    if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                        // Если нет кредов, выполняем запрос как есть (или бросаем ошибку, если креды обязательны)
                        return chain.proceed(originalRequest);
                    }
                    String auth = username + ":" + password;
                    String base64Auth = Base64.encodeToString(auth.getBytes(), Base64.NO_WRAP);
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Basic " + base64Auth)
                            .build();
                    return chain.proceed(newRequest);
                })
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

    // UseCases для перемещений (GetMoveListUseCase, GetDocumentMoveUseCase)
    // также будут автоматически предоставлены Hilt, так как их конструкторы
    // помечены @Inject и их зависимость (MoveRepository) теперь предоставляется этим модулем.
} 