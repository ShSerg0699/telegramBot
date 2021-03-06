package nsu.lsprod.botapi;

import nsu.lsprod.botapi.handlers.WatchingFilmInfoHandler;
import nsu.lsprod.botapi.handlers.watchingschedule.WatchingScheduleHandler;
import nsu.lsprod.cache.UserDataCache;
import nsu.lsprod.database.service.MovieScheduleService;
import nsu.lsprod.service.MainMenuService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramInterface {
    private BotStateContext botStateContext;
    private UserDataCache userDataCache;
    private MainMenuService mainMenuService;
    private WatchingScheduleHandler watchingScheduleHandler;
    private WatchingFilmInfoHandler watchingFilmInfoHandler;



    public TelegramInterface(BotStateContext botStateContext, UserDataCache userDataCache, MainMenuService mainMenuService, MovieScheduleService movieScheduleService, WatchingScheduleHandler watchingScheduleHandler, WatchingFilmInfoHandler watchingFilmInfoHandler) {
        this.botStateContext = botStateContext;
        this.userDataCache = userDataCache;
        this.mainMenuService = mainMenuService;
        this.watchingScheduleHandler = watchingScheduleHandler;
        this.watchingFilmInfoHandler = watchingFilmInfoHandler;
    }

    public BotApiMethod<?> handleUpdate(Update update) {
        SendMessage replyMessage = null;

        if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            return processCallbackQuery(callbackQuery);
        }


        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            replyMessage = handleInputMessage(message);
        }

        return replyMessage;
    }

    private SendMessage handleInputMessage(Message message) {
        String inputMsg = message.getText();
        int userId = message.getFrom().getId();
        BotState botState;
        SendMessage replyMessage;

        switch (inputMsg) {
            case "/start":
                botState = BotState.SHOW_MAIN_MENU;
                break;
            case "Посмотреть расписание":
                botState = BotState.SEARCHING_SCHEDULE;
                break;
            case "Посмотреть информацию о фильмах":
                botState = BotState.ASK_FILM_INFO;
                break;
            default:
                botState = BotState.SEARCHING_SCHEDULE;
                break;
        }
        userDataCache.setUsersCurrentBotState(userId, botState);
        replyMessage = botStateContext.processInputMessage(botState, message);
        return replyMessage;
    }


    private BotApiMethod<?> processCallbackQuery(CallbackQuery buttonQuery) {
        final long chatId = buttonQuery.getMessage().getChatId();
        final int userId = buttonQuery.getFrom().getId();
        BotApiMethod<?> callBackAnswer = mainMenuService.getMainMenuMessage(chatId, "Воспользуйтесь главным меню");

        String userInput = buttonQuery.getData();
        userInput = userInput.split("/")[0];
        String data = buttonQuery.getData().split("/")[1];
        if (userInput.equals("buttonDate") && userDataCache.getUsersCurrentBotState(userId) == BotState.ASK_DATE) {
            callBackAnswer = watchingScheduleHandler.askCinema(userId, data, chatId);
        } else if (userInput.equals("buttonCinema") && userDataCache.getUsersCurrentBotState(userId) == BotState.ASK_CINEMA) {
            callBackAnswer = watchingScheduleHandler.askFilm(userId, data, chatId);
        } else if (userInput.equals("buttonFilm") && userDataCache.getUsersCurrentBotState(userId) == BotState.ASK_FILM) {
            callBackAnswer = watchingScheduleHandler.watchingTable(userId, data, chatId);
        } else if (userInput.equals("buttonFilmInfo") && userDataCache.getUsersCurrentBotState(userId) == BotState.ASK_FILM_INFO) {
            callBackAnswer = watchingFilmInfoHandler.watchingFilmInfo(userId, data, chatId);
        }
        else {
            userDataCache.setUsersCurrentBotState(userId, BotState.SHOW_MAIN_MENU);
        }


        return callBackAnswer;
    }
}
